package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.service.ExpenseEntityMapper.toExpenses;
import static ro.pandemonium.expense.util.CollectionUtil.convertList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;

@ActivityScoped
public final class ExpenseSearchService {

    private final ExpenseDao expenseDao;
    private final List<Expense> changedExpenses = new ArrayList<>();
    private final List<Long> deletedExpenseIds = new ArrayList<>();
    private boolean initialized;
    private Filters filters;

    @Inject
    public ExpenseSearchService(final ExpenseDao expenseDao) {
        this.expenseDao = requireNonNull(expenseDao);
    }

    public List<Expense> initialize(final Filters filters) {
        requireNonNull(filters);

        this.filters = filters.copy();
        changedExpenses.clear();
        deletedExpenseIds.clear();
        initialized = true;

        return loadExpenses();
    }

    public List<Expense> update(final Expense expense) {
        requireInitialized();
        requireNonNull(expense);

        final int updatedCount = expenseDao.update(ExpenseEntityMapper.toEntity(expense, true));
        if (updatedCount != 1) {
            throw new IllegalStateException("Expense with id " + expense.getId() + " was not updated");
        }
        changedExpenses.add(expense);

        return loadExpenses();
    }

    private List<Expense> loadExpenses() {
        final List<Integer> expenseTypeIds = convertList(filters.expenseTypes(), ExpenseType::getDbId);

        return toExpenses(expenseDao.fetchExpenses(expenseTypeIds, filters.comments()))
                .stream()
                .sorted(Comparator.comparing(Expense::getDate)
                        .reversed()
                )
                .toList();
    }

    public void delete(final Long expenseId) {
        requireInitialized();
        requireNonNull(expenseId);

        final int deletedCount = expenseDao.delete(expenseId);
        if (deletedCount != 1) {
            throw new IllegalStateException("Expense with id " + expenseId + " was not deleted");
        }
        changedExpenses.removeIf(changedExpense -> expenseId.equals(changedExpense.getId()));
        deletedExpenseIds.add(expenseId);
    }

    public List<Expense> changedExpenses() {
        requireInitialized();

        return List.copyOf(changedExpenses);
    }

    public List<Long> deletedExpenseIds() {
        requireInitialized();

        return List.copyOf(deletedExpenseIds);
    }

    private void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Expense search service has not been initialized");
        }
    }
}
