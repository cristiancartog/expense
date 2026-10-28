package ro.pandemonium.expense.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public final class ExpenseSerialization {

    private ExpenseSerialization() {
    }

    public static String serialize(final Expense expense, final DateTimeFormatter dateFormatter) {
        return String.format(Locale.getDefault(), "%d,%s,%s,%.2f,%s",
                expense.getId(),
                expense.getExpenseType().name(),
                dateFormatter.format(expense.getDate()),
                expense.getValue(),
                expense.getComment());
    }

    public static Expense deserialize(final String serializedExpense, final DateTimeFormatter dateFormatter) {
        final String[] parts = serializedExpense.split(",");

        final Expense expense = new Expense();
        expense.setId(Long.valueOf(parts[0]));
        expense.setExpenseType(ExpenseType.valueOf(parts[1]));
        expense.setDate(LocalDate.parse(parts[2], dateFormatter));
        expense.setValue(Double.valueOf(parts[3]));
        expense.setComment(parts.length == 5 ? parts[4] : "");

        return expense;
    }
}
