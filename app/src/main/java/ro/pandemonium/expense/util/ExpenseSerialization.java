package ro.pandemonium.expense.util;

import static ro.pandemonium.expense.util.ExpenseMathUtil.amount;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public final class ExpenseSerialization {

    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private ExpenseSerialization() {
    }

    public static String serialize(final Expense expense) {
        return String.format(Locale.ROOT, "%d,%s,%s,%s,%s",
                expense.getId(),
                expense.getExpenseType().name(),
                CSV_DATE_FORMATTER.format(expense.getDate()),
                expense.getValue().toPlainString(),
                expense.getComment());
    }

    public static Expense deserialize(final String serializedExpense) {
        if (serializedExpense == null) {
            throw new IllegalArgumentException("Expense record is required");
        }
        final String[] parts = serializedExpense.split(",", 5);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Expense record is missing required fields");
        }

        return createExpense(parts);
    }

    private static Expense createExpense(final String[] parts) {
        final Expense expense = new Expense();
        expense.setId(Long.valueOf(parts[0]));
        expense.setExpenseType(ExpenseType.valueOf(parts[1]));
        expense.setDate(LocalDate.parse(parts[2], CSV_DATE_FORMATTER));
        expense.setValue(amount(new BigDecimal(parts[3])));
        expense.setComment(parts.length == 5 ? parts[4] : "");

        return expense;
    }
}
