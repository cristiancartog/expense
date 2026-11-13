package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;
import java.time.Month;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import ro.pandemonium.expense.model.ExpenseType;

public record YearComparisonState(
        ExpenseType expenseType,
        int year,
        boolean previousYearEnabled,
        boolean nextYearEnabled,
        BigDecimal currentYearTotal,
        BigDecimal previousYearTotal,
        Map<Month, Comparison> monthlyComparisons) {

    public YearComparisonState {
        requireNonNull(expenseType);

        final Map<Month, Comparison> monthlyComparisonsCopy = new EnumMap<>(Month.class);
        monthlyComparisonsCopy.putAll(requireNonNull(monthlyComparisons));
        monthlyComparisons = Collections.unmodifiableMap(monthlyComparisonsCopy);
    }
}
