package ro.pandemonium.expense.util;


import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseTest {

    @Test
    public void testSerialization() throws ParseException {
        SimpleDateFormat csvDateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());

        Expense expense = new Expense();
        expense.setId(12345678L);
        expense.setDate(new Date());
        expense.setComment("The big brown fox...");
        expense.setExpenseType(ExpenseType.OTHER);
        expense.setValue(123D);

        String expenseCsv = expense.toCsv(csvDateFormat);

        System.out.println(expenseCsv);

        System.out.println(Expense.fromCsv(expenseCsv, csvDateFormat));
    }
}
