package ro.pandemonium.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;

public class ExpenseSearchServiceTest {

    private final ExpenseDao expenseDao = mock(ExpenseDao.class);
    private final ExpenseSearchService service = new ExpenseSearchService(expenseDao);
    private final Filters filters = new Filters(List.of(FOOD), Set.of());

    @Test
    public void initializeLoadsAndSortsExpensesWithoutMutatingTheDaoResult() {
        final Expense laterExpense = expense(1L, "2026-04-20", 20D);
        final Expense earlierExpense = expense(2L, "2026-04-10", 10D);
        final List<Expense> daoResult = new ArrayList<>(List.of(laterExpense, earlierExpense));
        when(expenseDao.fetchExpenses(filters)).thenReturn(daoResult);

        final List<Expense> expenses = service.initialize(filters);

        assertEquals(List.of(laterExpense, earlierExpense), expenses);
        assertEquals(List.of(laterExpense, earlierExpense), daoResult);
        verify(expenseDao).fetchExpenses(filters);
    }

    @Test
    public void updatePersistsTheExpenseAndAccumulatesChangedExpenses() {
        final Expense firstExpense = expense(1L, "2026-04-10", 10D);
        final Expense secondExpense = expense(2L, "2026-04-20", 20D);
        when(expenseDao.fetchExpenses(filters)).thenReturn(List.of(firstExpense, secondExpense));
        service.initialize(filters);
        final Expense updatedFirstExpense = expense(1L, "2026-04-12", 15D);
        final Expense updatedSecondExpense = expense(2L, "2026-04-22", 25D);

        service.update(updatedFirstExpense);
        service.update(updatedSecondExpense);

        assertEquals(List.of(updatedFirstExpense, updatedSecondExpense), service.changedExpenses());
        verify(expenseDao).updateExpense(updatedFirstExpense);
        verify(expenseDao).updateExpense(updatedSecondExpense);
    }

    @Test
    public void returnedListsAreImmutable() {
        final Expense expense = expense(1L, "2026-04-10", 10D);
        when(expenseDao.fetchExpenses(filters)).thenReturn(List.of(expense));

        final List<Expense> expenses = service.initialize(filters);
        service.update(expense);
        final List<Expense> changedExpenses = service.changedExpenses();

        assertThrows(UnsupportedOperationException.class, expenses::clear);
        assertThrows(UnsupportedOperationException.class, () -> changedExpenses.add(expense));
    }

    @Test
    public void updateRequiresInitialization() {
        assertThrows(IllegalStateException.class, () -> service.update(expense(1L, "2026-04-10", 10D)));

        verifyNoInteractions(expenseDao);
    }

    @Test
    public void failedPersistenceDoesNotRecordTheExpenseAsChanged() {
        final Expense originalExpense = expense(1L, "2026-04-10", 10D);
        final Expense updatedExpense = expense(1L, "2026-04-10", 15D);
        when(expenseDao.fetchExpenses(filters)).thenReturn(List.of(originalExpense));
        service.initialize(filters);
        doThrow(new IllegalStateException("update failed"))
                .doNothing()
                .when(expenseDao)
                .updateExpense(updatedExpense);

        assertThrows(IllegalStateException.class, () -> service.update(updatedExpense));
        service.update(updatedExpense);

        assertEquals(List.of(updatedExpense), service.changedExpenses());
    }

    private Expense expense(final long id, final String date, final double value) {
        final Expense expense = new Expense();
        expense.setId(id);
        expense.setExpenseType(FOOD);
        expense.setDate(LocalDate.parse(date));
        expense.setValue(value);
        expense.setComment("");
        return expense;
    }
}
