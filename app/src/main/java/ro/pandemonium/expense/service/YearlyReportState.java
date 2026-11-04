package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import ro.pandemonium.expense.model.ExpenseType;

public record YearlyReportState(
        int year,
        PeriodMode periodMode,
        boolean previousYearEnabled,
        boolean nextYearEnabled,
        Comparison withSpecial,
        Comparison withoutSpecial,
        Map<ExpenseType, Comparison> byExpenseType) {

    public YearlyReportState {
        requireNonNull(periodMode);
        requireNonNull(withSpecial);
        requireNonNull(withoutSpecial);
        byExpenseType = Collections.unmodifiableMap(new EnumMap<>(requireNonNull(byExpenseType)));
    }

    public enum PeriodMode {
        FULL_YEAR,
        YEAR_TO_DATE,
        YEAR_FROM_DATE
    }

}
