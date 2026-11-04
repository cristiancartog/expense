package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

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

    public record MonthlyValues(YearMonth yearMonth, Map<ExpenseType, Double> values) {

        public MonthlyValues {
            requireNonNull(yearMonth);
            requireNonNull(values);

            final Map<ExpenseType, Double> valuesCopy = new EnumMap<>(ExpenseType.class);
            valuesCopy.putAll(values);
            values = Collections.unmodifiableMap(valuesCopy);
        }

        public double valueFor(final ExpenseType expenseType) {
            return values.getOrDefault(requireNonNull(expenseType), 0D);
        }
    }
}
