package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import ro.pandemonium.expense.db.ExpenseDao;
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

        final List<ExpenseMonthlySummary> summaries = requireNonNull(expenseDao.getMonthlySummary(expenseTypes));
        final List<MonthlyValues> monthlyValues = new ArrayList<>();
        for (ExpenseMonthlySummary summary : summaries) {
            final Map<ExpenseType, Double> values = new EnumMap<>(ExpenseType.class);
            for (ExpenseType expenseType : expenseTypes) {
                values.put(expenseType, summary.getValues().getOrDefault(expenseType, 0D));
            }
            monthlyValues.add(new MonthlyValues(summary.getYearMonth(), values));
        }

        return new ExpenseHistoryState(expenseTypes, monthlyValues);
    }
}
