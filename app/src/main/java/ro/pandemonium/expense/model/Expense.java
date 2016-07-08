package ro.pandemonium.expense.model;

import java.io.Serializable;
import java.util.Date;

public class Expense implements Serializable {

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
    public String toString() {
        return id + ", " + expenseType.name() + ", " + value  + ", " + date + ", " + comment;
    }
}
