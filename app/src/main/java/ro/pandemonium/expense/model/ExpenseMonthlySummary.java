package ro.pandemonium.expense.model;

import java.util.HashMap;
import java.util.Map;

public class ExpenseMonthlySummary {

    private String yearMonth;
    private Map<ExpenseType, Double> values = new HashMap<>();

    public ExpenseMonthlySummary(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public boolean isInSameMonth(String yearMonth) {
        return this.yearMonth.equals(yearMonth);
    }

    public void addExpense(final ExpenseType expenseType, final Double value) {
        values.put(expenseType, value);
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public Map<ExpenseType, Double> getValues() {
        return values;
    }
}
