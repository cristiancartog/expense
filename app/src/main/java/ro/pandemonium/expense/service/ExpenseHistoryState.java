package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.util.ExpenseMathUtil.ZERO;
import static ro.pandemonium.expense.util.ExpenseMathUtil.amount;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.model.ExpenseType;

public record ExpenseHistoryState(
        List<ExpenseType> expenseTypes,
        List<MonthlyValues> monthlyValues) {

    public ExpenseHistoryState {
        expenseTypes = List.copyOf(requireNonNull(expenseTypes));
        monthlyValues = List.copyOf(requireNonNull(monthlyValues));
    }

    public record MonthlyValues(YearMonth yearMonth, Map<ExpenseType, BigDecimal> values) {

        public MonthlyValues {
            requireNonNull(yearMonth);
            requireNonNull(values);

            final Map<ExpenseType, BigDecimal> valuesCopy = new EnumMap<>(ExpenseType.class);
            values.forEach((expenseType, value) -> valuesCopy.put(expenseType, amount(value)));
            values = Collections.unmodifiableMap(valuesCopy);
        }

        public BigDecimal valueFor(final ExpenseType expenseType) {
            return values.getOrDefault(requireNonNull(expenseType), ZERO);
        }
    }
}
