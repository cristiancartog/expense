package ro.pandemonium.expense.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ro.pandemonium.expense.model.ExpenseType;

class ExpenseConvertersTest {

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
