package ro.pandemonium.expense.model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class Expense implements Serializable {

    private Long id;
    private ExpenseType expenseType;
    private Double value;
    private Date date;
    private String comment;

    public ExpenseType getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(final ExpenseType expenseType) {
        this.expenseType = expenseType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(final Double value) {
        this.value = value;
    }

    public Long getTime() {
        return date.getTime();
    }

    public String getFormattedDate(final DateFormat dateFormat) {
        return date != null ? dateFormat.format(date) : "";
    }

    public void setDate(final Date date) {
        this.date = new Date(date.getTime());
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Expense && ((Expense) other).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + ", " + expenseType.name() + ", " + value + ", " + date + ", " + comment;
    }

    public String toCsv(final DateFormat dateFormat) {
        return String.format(Locale.getDefault(), "%d,%s,%s,%.2f,%s",
                id,
                expenseType.name(),
                dateFormat.format(date),
                value,
                comment);
    }

    public static Expense fromCsv(final String csv, final DateFormat dateFormat) throws ParseException {
        final String[] parts = csv.split(",");

        final Expense expense = new Expense();
        expense.setId(Long.valueOf(parts[0]));
        expense.setExpenseType(ExpenseType.valueOf(parts[1]));
        expense.setDate(dateFormat.parse(parts[2]));
        expense.setValue(Double.valueOf(parts[3]));
        expense.setComment(parts.length == 5 ? parts[4] : "");

        return expense;
    }
}
