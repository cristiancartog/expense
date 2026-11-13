package ro.pandemonium.expense.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ro.pandemonium.expense.model.ExpenseMonthlySummary;
import ro.pandemonium.expense.service.ExpenseHistoryState.MonthlyValues;

class ExpenseAmountBoundariesTest {

    @Test
    void summariesCannotIntroduceExcessPrecisionOrUnscaledDefaults() {
        final YearMonth month = YearMonth.of(2026, 1);
        final ExpenseMonthlySummary summary = new ExpenseMonthlySummary(month);
        summary.addExpense(FOOD, BigDecimal.ONE);

        assertEquals(new BigDecimal("1.00"), summary.getValues().get(FOOD));
        assertThrows(UnsupportedOperationException.class, () -> summary.getValues().put(FOOD, new BigDecimal("0.001")));
        assertThrows(IllegalArgumentException.class, () -> summary.addExpense(FOOD, new BigDecimal("0.001")));
        assertEquals(new BigDecimal("0.00"), new MonthlyValues(month, Map.of()).valueFor(FOOD));
        assertEquals(new BigDecimal("1.00"), new MonthlyValues(month, Map.of(FOOD, BigDecimal.ONE)).valueFor(FOOD));
        assertThrows(IllegalArgumentException.class, () -> new MonthlyValues(month, Map.of(FOOD, new BigDecimal("0.001"))));
    }
}
