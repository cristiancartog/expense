package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.time.YearMonth;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.MonthlySummaryRow;
import ro.pandemonium.expense.model.ExpenseMonthlySummary;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.service.ExpenseHistoryState.MonthlyValues;

@ActivityScoped
public final class ExpenseHistoryService {

    private final ExpenseDao expenseDao;

    @Inject
    public ExpenseHistoryService(final ExpenseDao expenseDao) {
        this.expenseDao = requireNonNull(expenseDao);
    }

    public ExpenseHistoryState load(final List<ExpenseType> requestedExpenseTypes) {
        final List<ExpenseType> expenseTypes = List.copyOf(requireNonNull(requestedExpenseTypes));
        if (expenseTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one expense type is required");
        }

        final List<Integer> expenseTypeIds = expenseTypes.stream()
                                                         .map(ExpenseType::getDbId)
                                                         .toList();
        final Map<YearMonth, ExpenseMonthlySummary> summariesByMonth = new java.util.TreeMap<>();
        for (MonthlySummaryRow row : expenseDao.getMonthlySummary(expenseTypeIds)) {
            final YearMonth yearMonth = YearMonth.parse(row.month);
            summariesByMonth.computeIfAbsent(yearMonth, ExpenseMonthlySummary::new)
                            .addExpense(ExpenseType.byDbId(row.expenseType), row.total);
        }
        final List<MonthlyValues> monthlyValues = new ArrayList<>();
        for (ExpenseMonthlySummary summary : summariesByMonth.values()) {
            final Map<ExpenseType, Double> values = new EnumMap<>(ExpenseType.class);
            for (ExpenseType expenseType : expenseTypes) {
                values.put(expenseType, summary.getValues().getOrDefault(expenseType, 0D));
            }
            monthlyValues.add(new MonthlyValues(summary.getYearMonth(), values));
        }

        return new ExpenseHistoryState(expenseTypes, monthlyValues);
    }
}
