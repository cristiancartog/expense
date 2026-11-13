package ro.pandemonium.expense.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

class ExpenseUtilTest {

    @Test
    void sumsAmountsAndCategoriesExactly() {
        final List<Expense> expenses = List.of(expense(FOOD, "0.10"), expense(FOOD, "0.20"), expense(PETROL, "999999999.99"));

        assertEquals(new BigDecimal("1000000000.29"), ExpenseUtil.computeTotalValue(expenses));
        assertEquals(Map.of(FOOD, new BigDecimal("0.30"), PETROL, new BigDecimal("999999999.99")), ExpenseUtil.computeExpenseSumByCategory(expenses));
        assertEquals(new BigDecimal("0.00"), ExpenseUtil.computeTotalValue(List.of()));
        assertTrue(ExpenseUtil.computeExpenseSumByCategory(List.of()).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.01", "12.5000", "999999999.99"})
    void acceptsNonnegativeAmountsWithAtMostTwoSignificantDecimalPlaces(final String amount) {
        assertTrue(ExpenseUtil.isValidValue(new BigDecimal(amount)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "0.001", "12.500000000000000001", "999999999.990000001", "1000000000"})
    void rejectsAmountsOutsideTheSupportedRangeOrPrecision(final String amount) {
        assertFalse(ExpenseUtil.isValidValue(new BigDecimal(amount)));
    }

    @Test
    void rejectsMissingAmounts() {
        assertFalse(ExpenseUtil.isValidValue(null));
    }

    private Expense expense(final ExpenseType expenseType, final String amount) {
        final Expense expense = new Expense();
        expense.setExpenseType(expenseType);
        expense.setValue(new BigDecimal(amount));
        return expense;
    }
}
