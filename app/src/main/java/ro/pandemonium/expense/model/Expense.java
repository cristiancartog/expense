package ro.pandemonium.expense.model;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ro.pandemonium.expense.Constants;

public class Expense implements Serializable {

    private static final SimpleDateFormat CSV_DATE_DATE_FORMAT = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());

    private Long id;
    private ExpenseType expenseType;
    private Double value;
    private Date date;
    private String comment;

    public ExpenseType getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(ExpenseType expenseType) {
        this.expenseType = expenseType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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
    public String toString() {
        return id + ", " + expenseType.name() + ", " + value + ", " + date + ", " + comment;
    }

    public String toCsv() {
        return String.format(Locale.getDefault(), "%d,%s,%s,%.2f,%s",
                id,
                expenseType.name(),
                CSV_DATE_DATE_FORMAT.format(date),
                value,
                comment);
    }

    public static Expense fromCsv(final String csv) throws ParseException {
        final String[] parts = csv.split(",");

        final Expense expense = new Expense();
        expense.setId(Long.valueOf(parts[0]));
        expense.setExpenseType(ExpenseType.valueOf(parts[1]));
        expense.setDate(CSV_DATE_DATE_FORMAT.parse(parts[2]));
        expense.setValue(Double.valueOf(parts[3]));
        expense.setComment(parts.length == 5 ? parts[4] : "");

        return expense;
    }
}
