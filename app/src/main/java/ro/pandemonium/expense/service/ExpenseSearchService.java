package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;

@ActivityScoped
public final class ExpenseSearchService {

    private final ExpenseDao expenseDao;
    private final List<Expense> changedExpenses = new ArrayList<>();
    private boolean initialized;

    @Inject
    public ExpenseSearchService(final ExpenseDao expenseDao) {
        this.expenseDao = requireNonNull(expenseDao);
    }

    public List<Expense> initialize(final Filters filters) {
        requireNonNull(filters);

        final List<Expense> loadedExpenses = new ArrayList<>(requireNonNull(expenseDao.fetchExpenses(filters)));
        loadedExpenses.sort(Comparator.comparing(Expense::getDate)
                                      .reversed());

        changedExpenses.clear();
        initialized = true;
        return List.copyOf(loadedExpenses);
    }

    public void update(final Expense expense) {
        requireInitialized();
        requireNonNull(expense);

        expenseDao.updateExpense(expense);
        changedExpenses.add(expense);
    }

    public List<Expense> changedExpenses() {
        requireInitialized();
        return List.copyOf(changedExpenses);
    }

    private void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Expense search service has not been initialized");
        }
    }
}
