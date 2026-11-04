package ro.pandemonium.expense.service;

import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.OTHER;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class YearComparisonServiceTest {

    private static final double DELTA = 0.000001D;
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), UTC);

    @Test
    public void reportNormalizesMonthsAndCalculatesTotalsAndVariations() {
        final MockMonthlySummaryStore store = storeWithBounds("2022-03-10", "2025-08-20");
        store.values.put(2023, Map.of("01", 100D, "03", 50D));
        store.values.put(2024, Map.of("01", 150D, "02", 25D));

        final YearComparisonState state = service(store.expenseDao).initialize(FOOD, 2024);

        assertEquals(FOOD, state.expenseType());
        assertEquals(2024, state.year());
        assertTrue(state.previousYearEnabled());
        assertTrue(state.nextYearEnabled());
        assertEquals(175D, state.currentYearTotal(), DELTA);
        assertEquals(150D, state.previousYearTotal(), DELTA);
        assertComparison(state, JANUARY, 150D, 100D, 0.5D);
        assertFalse(comparison(state, FEBRUARY).variation().isPresent());
        assertEquals(12, state.monthlyComparisons().size());
        assertThrows(UnsupportedOperationException.class,
                () -> state.monthlyComparisons().put(JANUARY, comparison(state, JANUARY)));
        verify(store.expenseDao).getMonthlySummaryInYear(FOOD, 2024);
        verify(store.expenseDao).getMonthlySummaryInYear(FOOD, 2023);
    }

    @Test
    public void dropToZeroProducesNegativeOneHundredPercentVariation() {
        final MockMonthlySummaryStore store = storeWithBounds("2023-01-01", "2024-12-31");
        store.values.put(2023, Map.of("01", 40D));

        final YearComparisonState state = service(store.expenseDao).initialize(FOOD, 2024);

        assertComparison(state, JANUARY, 0D, 40D, -1D);
    }

    @Test
    public void navigationHonorsYearsThatHaveAComparablePreviousYear() {
        final MockMonthlySummaryStore store = storeWithBounds("2022-03-10", "2025-08-20");
        final YearComparisonService service = service(store.expenseDao);
        service.initialize(FOOD, 2025);

        assertEquals(2024, service.previousYear().year());
        assertEquals(2023, service.previousYear().year());
        final YearComparisonState lowerBound = service.previousYear();
        assertEquals(2023, lowerBound.year());
        assertFalse(lowerBound.previousYearEnabled());

        assertEquals(2024, service.nextYear().year());
        assertEquals(2025, service.nextYear().year());
        final YearComparisonState upperBound = service.nextYear();
        assertEquals(2025, upperBound.year());
        assertFalse(upperBound.nextYearEnabled());
    }

    @Test
    public void failedNavigationDoesNotCommitTheRequestedYear() {
        final MockMonthlySummaryStore store = storeWithBounds("2022-01-01", "2025-12-31");
        final YearComparisonService service = service(store.expenseDao);
        service.initialize(FOOD, 2024);
        store.failQueries = true;

        assertThrows(IllegalStateException.class, service::nextYear);

        store.failQueries = false;
        assertEquals(2023, service.previousYear().year());
    }

    @Test
    public void emptyDaoUsesCurrentYearForNavigationBounds() {
        final MockMonthlySummaryStore store = new MockMonthlySummaryStore();

        final YearComparisonState state = service(store.expenseDao).initialize(FOOD, 2026);

        assertEquals(2026, state.year());
        assertFalse(state.previousYearEnabled());
        assertFalse(state.nextYearEnabled());
        assertEquals(0D, state.currentYearTotal(), DELTA);
        assertEquals(0D, state.previousYearTotal(), DELTA);
        assertTrue(state.monthlyComparisons().values().stream()
                .allMatch(comparison -> comparison.variation().isEmpty()));
    }

    @Test
    public void navigationRequiresInitialization() {
        final YearComparisonService service = service(new MockMonthlySummaryStore().expenseDao);

        assertThrows(IllegalStateException.class, service::nextYear);
        assertThrows(IllegalStateException.class, service::previousYear);
    }

    private YearComparisonService service(final ExpenseDao expenseDao) {
        return new YearComparisonService(expenseDao, FIXED_CLOCK);
    }

    private MockMonthlySummaryStore storeWithBounds(final String earliest, final String latest) {
        final MockMonthlySummaryStore store = new MockMonthlySummaryStore();
        when(store.expenseDao.getEarliestEntry())
                .thenReturn(Optional.of(expense(-1, OTHER, earliest)));
        when(store.expenseDao.getLatestEntry())
                .thenReturn(Optional.of(expense(-2, OTHER, latest)));
        return store;
    }

    private static Expense expense(final long id, final ExpenseType type, final String date) {
        final Expense expense = new Expense();
        expense.setId(id);
        expense.setExpenseType(type);
        expense.setDate(LocalDate.parse(date));
        expense.setValue(0D);
        expense.setComment("");
        return expense;
    }

    private void assertComparison(final YearComparisonState state,
                                  final Month month,
                                  final double current,
                                  final double previous,
                                  final double variation) {
        final Comparison comparison = comparison(state, month);
        assertEquals(current, comparison.currentValue(), DELTA);
        assertEquals(previous, comparison.previousValue(), DELTA);
        assertTrue(comparison.variation().isPresent());
        assertEquals(variation, comparison.variation().getAsDouble(), DELTA);
    }

    private Comparison comparison(final YearComparisonState state, final Month month) {
        return state.monthlyComparisons().get(month);
    }

    private static final class MockMonthlySummaryStore {
        private final ExpenseDao expenseDao = mock(ExpenseDao.class);
        private final Map<Integer, Map<String, Double>> values = new HashMap<>();
        private boolean failQueries;

        private MockMonthlySummaryStore() {
            when(expenseDao.getEarliestEntry()).thenReturn(Optional.empty());
            when(expenseDao.getLatestEntry()).thenReturn(Optional.empty());
            doAnswer(invocation -> {
                if (failQueries) {
                    throw new IllegalStateException("query failed");
                }
                final int year = invocation.getArgument(1);
                return new HashMap<>(values.getOrDefault(year, Map.of()));
            }).when(expenseDao).getMonthlySummaryInYear(eq(FOOD), anyInt());
        }
    }
}
