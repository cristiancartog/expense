package ro.pandemonium.expense.util;


import static org.junit.Assert.assertEquals;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_DB;

import org.junit.Test;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseTest {

    @Test
    public void testSerialization() {
        final DateTimeFormatter csvDateFormat = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_DB, Locale.getDefault());

        Expense expense = new Expense();
        expense.setId(12345678L);
        expense.setDate(LocalDate.of(2000, Month.MARCH, 31));
        expense.setComment("The big brown fox...");
        expense.setExpenseType(ExpenseType.OTHER);
        expense.setValue(123D);

        String expenseCsv = expense.toCsv(csvDateFormat);

        Expense sameExpense = Expense.fromCsv(expenseCsv, csvDateFormat);

        assertEquals(expense.getId(), sameExpense.getId());
        assertEquals(expense.getComment(), sameExpense.getComment());
        assertEquals(expense.getDate(), sameExpense.getDate());
        assertEquals(expense.getExpenseType(), sameExpense.getExpenseType());
        assertEquals(expense.getValue(), sameExpense.getValue());
    }
}
