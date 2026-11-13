package ro.pandemonium.expense.model;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.util.ExpenseMathUtil.amount;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class ExpenseMonthlySummary {

    private final YearMonth yearMonth;
    private final Map<ExpenseType, BigDecimal> values = new EnumMap<>(ExpenseType.class);

    public ExpenseMonthlySummary(final YearMonth yearMonth) {
        this.yearMonth = requireNonNull(yearMonth);
    }

    public void addExpense(final ExpenseType expenseType, final BigDecimal value) {
        values.put(requireNonNull(expenseType), amount(value));
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public Map<ExpenseType, BigDecimal> getValues() {
        return Collections.unmodifiableMap(values);
    }
}
