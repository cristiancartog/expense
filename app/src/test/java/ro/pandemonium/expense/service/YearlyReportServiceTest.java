package ro.pandemonium.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.time.ZoneOffset.UTC;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.OTHER;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;
import static ro.pandemonium.expense.service.YearlyReportState.PeriodMode.FULL_YEAR;
import static ro.pandemonium.expense.service.YearlyReportState.PeriodMode.YEAR_FROM_DATE;
import static ro.pandemonium.expense.service.YearlyReportState.PeriodMode.YEAR_TO_DATE;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.ExpenseEntity;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class YearlyReportServiceTest {

    private static final double DELTA = 0.000001D;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), UTC);

    @Test
    public void fullYearReportCalculatesTotalsCategoriesAndVariations() {
        final MockExpenseStore store = storeWithBounds("2022-03-10", "2025-08-20");
        store.expenses.add(expense(1, FOOD, 100, "2023-02-01"));
        store.expenses.add(expense(2, SPECIAL, 50, "2023-04-01"));
        store.expenses.add(expense(3, FOOD, 150, "2024-02-01"));
        store.expenses.add(expense(4, OTHER, 25, "2024-06-01"));
        store.expenses.add(expense(5, SPECIAL, 75, "2024-09-01"));

        final YearlyReportState state = service(store.expenseDao).initialize(2024);

        assertEquals(2024, state.year());
        assertEquals(FULL_YEAR, state.periodMode());
        assertTrue(state.previousYearEnabled());
        assertTrue(state.nextYearEnabled());
        assertComparison(state.withSpecial(), 250, 150, 2D / 3D);
        assertComparison(state.withoutSpecial(), 175, 100, 0.75D);
        assertComparison(state.byExpenseType().get(FOOD), 150, 100, 0.5D);
        assertComparison(state.byExpenseType().get(SPECIAL), 75, 50, 0.5D);
        assertEquals(0D, state.byExpenseType().get(OTHER).previousValue(), DELTA);
        assertFalse(state.byExpenseType().get(OTHER).variation().isPresent());
        verifyRange(store.expenseDao, "2024-01-01", "2024-12-31");
        verifyRange(store.expenseDao, "2023-01-01", "2023-12-31");
    }

    @Test
    public void latestYearDefaultsToYearToDateUsingMatchingCutoffs() {
        final MockExpenseStore store = storeWithBounds("2022-03-10", "2025-08-20");

        final YearlyReportState state = service(store.expenseDao).initialize(2025);

        assertEquals(YEAR_TO_DATE, state.periodMode());
        assertFalse(state.nextYearEnabled());
        verifyRange(store.expenseDao, "2025-01-01", "2025-08-20");
        verifyRange(store.expenseDao, "2024-01-01", "2024-08-20");
    }

    @Test
    public void yearToDateUsesAValidPriorYearCutoffForLeapDay() {
        final MockExpenseStore store = storeWithBounds("2022-01-01", "2024-02-29");

        service(store.expenseDao).initialize(2024);

        verifyRange(store.expenseDao, "2024-01-01", "2024-02-29");
        verifyRange(store.expenseDao, "2023-01-01", "2023-02-28");
    }

    @Test
    public void firstComparableYearCanToggleYearFromDate() {
        final MockExpenseStore store = storeWithBounds("2022-03-10", "2025-08-20");
        final YearlyReportService service = service(store.expenseDao);
        service.initialize(2023);
        clearInvocations(store.expenseDao);

        final YearlyReportState state = service.toggleComparisonPeriod();

        assertEquals(YEAR_FROM_DATE, state.periodMode());
        verifyRange(store.expenseDao, "2023-03-10", "2023-12-31");
        verifyRange(store.expenseDao, "2022-03-10", "2022-12-31");
        assertEquals(FULL_YEAR, service.toggleComparisonPeriod().periodMode());
    }

    @Test
    public void navigationHonorsBoundsAndResetsComparisonPeriod() {
        final MockExpenseStore store = storeWithBounds("2022-03-10", "2025-08-20");
        final YearlyReportService service = service(store.expenseDao);
        service.initialize(2025);

        final YearlyReportState previous = service.previousYear();
        assertEquals(2024, previous.year());
        assertEquals(FULL_YEAR, previous.periodMode());

        assertEquals(2023, service.previousYear().year());
        final YearlyReportState lowerBound = service.previousYear();
        assertEquals(2023, lowerBound.year());
        assertFalse(lowerBound.previousYearEnabled());

        assertEquals(2024, service.nextYear().year());
        assertEquals(2025, service.nextYear().year());
        final YearlyReportState upperBound = service.nextYear();
        assertEquals(2025, upperBound.year());
        assertFalse(upperBound.nextYearEnabled());
    }

    @Test
    public void zeroPreviousValueProducesUnavailableVariation() {
        final MockExpenseStore store = storeWithBounds("2023-01-01", "2024-12-31");
        store.expenses.add(expense(1, FOOD, 20, "2024-02-01"));

        final YearlyReportState state = service(store.expenseDao).initialize(2024);

        assertFalse(state.withSpecial().variation().isPresent());
        assertFalse(state.byExpenseType().get(FOOD).variation().isPresent());
    }

    @Test
    public void failedNavigationDoesNotCommitTheRequestedYear() {
        final MockExpenseStore store = storeWithBounds("2022-01-01", "2025-12-31");
        final YearlyReportService service = service(store.expenseDao);
        service.initialize(2024);
        store.failQueries = true;

        assertThrows(IllegalStateException.class, service::nextYear);

        store.failQueries = false;
        assertEquals(2023, service.previousYear().year());
    }

    @Test
    public void emptyDaoUsesCurrentDateAndReturnsCompleteImmutableState() {
        final MockExpenseStore store = new MockExpenseStore();

        final YearlyReportState state = service(store.expenseDao).initialize(2026);

        assertEquals(YEAR_TO_DATE, state.periodMode());
        assertFalse(state.previousYearEnabled());
        assertFalse(state.nextYearEnabled());
        assertEquals(ExpenseType.values().length, state.byExpenseType().size());
        assertThrows(UnsupportedOperationException.class,
                () -> state.byExpenseType().put(FOOD, state.withSpecial()));
        verifyRange(store.expenseDao, "2026-01-01", "2026-10-28");
        verifyRange(store.expenseDao, "2025-01-01", "2025-10-28");
    }

    private YearlyReportService service(final ExpenseDao expenseDao) {
        return new YearlyReportService(expenseDao, FIXED_CLOCK);
    }

    private MockExpenseStore storeWithBounds(final String earliest, final String latest) {
        final MockExpenseStore store = new MockExpenseStore();
        when(store.expenseDao.getEarliestEntry())
                .thenReturn(entity(expense(-1, OTHER, 0, earliest)));
        when(store.expenseDao.getLatestEntry())
                .thenReturn(entity(expense(-2, OTHER, 0, latest)));
        return store;
    }

    private static Expense expense(final long id, final ExpenseType type,
                                   final double value, final String date) {
        final Expense expense = new Expense();
        expense.setId(id);
        expense.setExpenseType(type);
        expense.setValue(value);
        expense.setDate(LocalDate.parse(date));
        expense.setComment("");
        return expense;
    }

    private static ExpenseEntity entity(final Expense expense) {
        return ExpenseEntityMapper.toEntity(expense, true);
    }

    private void assertComparison(final Comparison comparison,
                                  final double current,
                                  final double previous,
                                  final double variation) {
        assertEquals(current, comparison.currentValue(), DELTA);
        assertEquals(previous, comparison.previousValue(), DELTA);
        assertTrue(comparison.variation().isPresent());
        assertEquals(variation, comparison.variation().getAsDouble(), DELTA);
    }

    private void verifyRange(final ExpenseDao expenseDao, final String start, final String end) {
        verify(expenseDao).findExpensesInInterval(LocalDate.parse(start), LocalDate.parse(end));
    }

    private static final class MockExpenseStore {
        private final ExpenseDao expenseDao = mock(ExpenseDao.class);
        private final List<Expense> expenses = new ArrayList<>();
        private boolean failQueries;

        private MockExpenseStore() {
            doAnswer(invocation -> {
                if (failQueries) {
                    throw new IllegalStateException("query failed");
                }
                final LocalDate startInclusive = invocation.getArgument(0);
                final LocalDate endInclusive = invocation.getArgument(1);
                return expenses.stream()
                        .filter(expense -> !expense.getDate().isBefore(startInclusive))
                        .filter(expense -> !expense.getDate().isAfter(endInclusive))
                        .map(YearlyReportServiceTest::entity)
                        .toList();
            }).when(expenseDao).findExpensesInInterval(any(LocalDate.class), any(LocalDate.class));
        }
    }
}
