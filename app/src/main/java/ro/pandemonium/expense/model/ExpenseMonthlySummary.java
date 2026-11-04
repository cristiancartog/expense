package ro.pandemonium.expense.model;

import static java.util.Objects.requireNonNull;

import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

public class ExpenseMonthlySummary {

    private final YearMonth yearMonth;
    private final Map<ExpenseType, Double> values = new EnumMap<>(ExpenseType.class);

    public ExpenseMonthlySummary(final YearMonth yearMonth) {
        this.yearMonth = requireNonNull(yearMonth);
    }

    public void addExpense(final ExpenseType expenseType, final Double value) {
        values.put(expenseType, value);
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public Map<ExpenseType, Double> getValues() {
        return values;
    }
}
