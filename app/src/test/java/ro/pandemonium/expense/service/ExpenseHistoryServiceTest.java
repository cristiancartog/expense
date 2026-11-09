package ro.pandemonium.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.MonthlySummaryRow;
import ro.pandemonium.expense.model.ExpenseMonthlySummary;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.service.ExpenseHistoryState.MonthlyValues;

public class ExpenseHistoryServiceTest {

    private static final double DELTA = 0.000001D;

    private final ExpenseDao expenseDao = mock(ExpenseDao.class);
    private final ExpenseHistoryService service = new ExpenseHistoryService(expenseDao);

    @Test
    public void loadCreatesACompleteSeriesForEveryRequestedExpenseType() {
        final List<ExpenseType> expenseTypes = List.of(FOOD, PETROL);
        when(expenseDao.getMonthlySummary(List.of(FOOD.getDbId(), PETROL.getDbId()))).thenReturn(List.of(
                row(FOOD, "2026-01", 125D),
                row(PETROL, "2026-02", 80D)
        ));

        final ExpenseHistoryState state = service.load(expenseTypes);

        assertEquals(expenseTypes, state.expenseTypes());
        assertEquals(2, state.monthlyValues().size());
        assertMonth(state.monthlyValues().get(0), YearMonth.of(2026, 1), 125D, 0D);
        assertMonth(state.monthlyValues().get(1), YearMonth.of(2026, 2), 0D, 80D);
        verify(expenseDao).getMonthlySummary(List.of(FOOD.getDbId(), PETROL.getDbId()));
    }

    @Test
    public void returnedStateDoesNotExposeMutableCollections() {
        final List<ExpenseType> requestedTypes = new ArrayList<>(List.of(FOOD));
        final MonthlySummaryRow summary = row(FOOD, "2026-01", 10D);
        when(expenseDao.getMonthlySummary(List.of(FOOD.getDbId()))).thenReturn(List.of(summary));

        final ExpenseHistoryState state = service.load(requestedTypes);
        requestedTypes.add(PETROL);
        summary.total = 20D;

        assertEquals(List.of(FOOD), state.expenseTypes());
        assertEquals(10D, state.monthlyValues().getFirst().valueFor(FOOD), DELTA);
        assertThrows(UnsupportedOperationException.class, () -> state.expenseTypes().add(PETROL));
        assertThrows(UnsupportedOperationException.class, () -> state.monthlyValues().getFirst().values().put(FOOD, 20D));
    }

    @Test
    public void emptyExpenseTypeSelectionIsRejectedBeforeQueryingTheDatabase() {
        assertThrows(IllegalArgumentException.class, () -> service.load(List.of()));

        verifyNoInteractions(expenseDao);
    }

    private MonthlySummaryRow row(final ExpenseType expenseType, final String month, final double total) {
        final MonthlySummaryRow row = new MonthlySummaryRow();
        row.expenseType = expenseType.getDbId();
        row.month = month;
        row.total = total;
        return row;
    }

    private void assertMonth(final MonthlyValues values,
                             final YearMonth yearMonth,
                             final double food,
                             final double petrol) {
        assertEquals(yearMonth, values.yearMonth());
        assertEquals(food, values.valueFor(FOOD), DELTA);
        assertEquals(petrol, values.valueFor(PETROL), DELTA);
    }
}
