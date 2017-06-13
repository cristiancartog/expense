package ro.pandemonium.expense.model;

public class YearlyReportParams {

    private ExpenseType expenseType;
    private int year;

    public YearlyReportParams(final ExpenseType expenseType, final int year) {
        this.expenseType = expenseType;
        this.year = year;
    }

    public ExpenseType getExpenseType() {
        return expenseType;
    }

    public int getYear() {
        return year;
    }

}
