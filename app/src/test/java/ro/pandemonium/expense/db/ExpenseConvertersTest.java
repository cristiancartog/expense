package ro.pandemonium.expense.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ro.pandemonium.expense.model.ExpenseType;

class ExpenseConvertersTest {

    @Test
    void convertsAmountsAndTotalsToExactScaledIntegers() {
        assertEquals(29L, ExpenseConverters.valueToScaledInteger(new BigDecimal("0.29")));
        assertEquals(1250L, ExpenseConverters.valueToScaledInteger(new BigDecimal("12.5000")));
        assertEquals(99999999999L, ExpenseConverters.valueToScaledInteger(new BigDecimal("999999999.99")));
        assertEquals(new BigDecimal("0.29"), ExpenseConverters.scaledIntegerToValue(29L));
        assertEquals(new BigDecimal("1999999999.98"), ExpenseConverters.scaledIntegerToValue(199999999998L));
        assertNull(ExpenseConverters.valueToScaledInteger(null));
        assertNull(ExpenseConverters.scaledIntegerToValue(null));
    }

    @Test
    void refusesToRoundOrOverflowAmountsWhenPersisting() {
        assertThrows(ArithmeticException.class, () -> ExpenseConverters.valueToScaledInteger(new BigDecimal("0.001")));
        assertThrows(ArithmeticException.class, () -> ExpenseConverters.valueToScaledInteger(new BigDecimal("92233720368547758.08")));
    }

    @Test
    void convertsEveryExpenseType() {
        for (ExpenseType expenseType : ExpenseType.values()) {
            assertEquals(expenseType.getDbId(), ExpenseConverters.expenseTypeToDbId(expenseType));
            assertEquals(expenseType, ExpenseConverters.dbIdToExpenseType(expenseType.getDbId()));
        }
        assertNull(ExpenseConverters.expenseTypeToDbId(null));
        assertNull(ExpenseConverters.dbIdToExpenseType(null));
    }

    @Test
    void convertsLocalDateAsIsoText() {
        final LocalDate date = LocalDate.of(2026, 11, 9);

        assertEquals("2026-11-09", ExpenseConverters.localDateToText(date));
        assertEquals(date, ExpenseConverters.textToLocalDate("2026-11-09"));
        assertNull(ExpenseConverters.localDateToText(null));
        assertNull(ExpenseConverters.textToLocalDate(null));
    }
}
