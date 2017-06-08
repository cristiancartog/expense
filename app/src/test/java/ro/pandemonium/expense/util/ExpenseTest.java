package ro.pandemonium.expense.util;


import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

import static junit.framework.Assert.assertEquals;

public class ExpenseTest {

    @Test
    public void testSerialization() throws ParseException {
        SimpleDateFormat csvDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2000);
        calendar.set(Calendar.MONTH, 2);
        calendar.set(Calendar.DAY_OF_MONTH, 31);

        Expense expense = new Expense();
        expense.setId(12345678L);
        expense.setDate(calendar.getTime());
        expense.setComment("The big brown fox...");
        expense.setExpenseType(ExpenseType.OTHER);
        expense.setValue(123D);

        String expenseCsv = expense.toCsv(csvDateFormat);

        Expense sameExpense = Expense.fromCsv(expenseCsv, csvDateFormat);

        assertEquals(expense.getId(), sameExpense.getId());
        assertEquals(expense.getComment(), sameExpense.getComment());
//        assertEquals(expense.getTime(), sameExpense.getTime());
        assertEquals(expense.getExpenseType(), sameExpense.getExpenseType());
        assertEquals(expense.getValue(), sameExpense.getValue());
    }
}
