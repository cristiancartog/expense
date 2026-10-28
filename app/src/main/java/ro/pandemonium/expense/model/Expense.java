package ro.pandemonium.expense.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Expense implements Serializable {

    private Long id;
    private ExpenseType expenseType;
    private Double value;
    private LocalDate date;
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

    public LocalDate getDate() {
        return date;
    }

    public String getFormattedDate(final DateTimeFormatter formatter) {
        return date != null ? formatter.format(date) : "";
    }

    public void setDate(final LocalDate date) {
        this.date = LocalDate.from(date);
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

    @NonNull
    @Override
    public String toString() {
        return id + ", " + expenseType.name() + ", " + value + ", " + date + ", " + comment;
    }

}
