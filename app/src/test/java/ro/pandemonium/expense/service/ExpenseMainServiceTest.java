package ro.pandemonium.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;
import static ro.pandemonium.expense.service.ExpenseMainUpdate.OperationType.ADD;
import static ro.pandemonium.expense.service.ExpenseMainUpdate.OperationType.NONE;
import static ro.pandemonium.expense.service.ExpenseMainUpdate.OperationType.REMOVE;
import static ro.pandemonium.expense.service.ExpenseMainUpdate.OperationType.UPDATE;

import android.content.Context;
import android.net.Uri;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.ExpenseEntity;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.util.FileUtil;

public class ExpenseMainServiceTest {

    private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 11);

    private final ExpenseDao expenseDao = mock(ExpenseDao.class);
    private final Context context = mock(Context.class);
    private final AtomicLong currentTimeMillis = new AtomicLong(2_000L);
    private final ExpenseMainService service = new ExpenseMainService(
            expenseDao,
            context,
            Clock.fixed(Instant.parse("2026-11-04T10:15:30Z"), ZoneOffset.UTC),
            () -> currentTimeMillis.getAndAdd(1_001L)
    );

    @Test
    public void initializeLoadsTheCurrentMonthAndSortsTheDaoResult() {
        final Expense laterExpense = expense(1L, FOOD, "2026-11-20", 20D);
        final Expense earlierExpense = expense(2L, PETROL, "2026-11-10", 10D);
        final List<ExpenseEntity> daoResult = new ArrayList<>(List.of(entity(laterExpense), entity(earlierExpense)));
        when(expenseDao.fetchExpensesInMonth(SPECIAL.getDbId(), CURRENT_MONTH.toString())).thenReturn(daoResult);

        final ExpenseMainState state = service.initialize();

        assertEquals(CURRENT_MONTH, state.yearMonth());
        assertEquals(List.of(earlierExpense, laterExpense), state.expenses());
        assertEquals(List.of(laterExpense.getId(), earlierExpense.getId()), daoResult.stream().map(entity -> entity.id).toList());
        assertThrows(UnsupportedOperationException.class, state.expenses()::clear);
    }

    @Test
    public void monthNavigationLoadsTheRequestedMonths() {
        when(expenseDao.fetchExpensesInMonth(eq(SPECIAL.getDbId()), any(String.class))).thenReturn(new ArrayList<>());
        service.initialize();

        assertEquals(YearMonth.of(2026, 12), service.nextMonth().yearMonth());
        assertEquals(YearMonth.of(2026, 11), service.previousMonth().yearMonth());
        assertEquals(YearMonth.of(2025, 3), service.selectMonth(YearMonth.of(2025, 3)).yearMonth());

        verify(expenseDao).fetchExpensesInMonth(SPECIAL.getDbId(), "2026-12");
        verify(expenseDao, times(2)).fetchExpensesInMonth(SPECIAL.getDbId(), "2026-11");
        verify(expenseDao).fetchExpensesInMonth(SPECIAL.getDbId(), "2025-03");
    }

    @Test
    public void statefulOperationsRequireInitialization() {
        final Expense expense = expense(null, FOOD, "2026-11-04", 10D);

        assertThrows(IllegalStateException.class, service::nextMonth);
        assertThrows(IllegalStateException.class, service::previousMonth);
        assertThrows(IllegalStateException.class, () -> service.selectMonth(YearMonth.of(2026, 11)));
        assertThrows(IllegalStateException.class, service::reload);
        assertThrows(IllegalStateException.class, () -> service.saveExpense(expense));

        verifyNoInteractions(expenseDao);
    }

    @Test
    public void savingANewRegularExpenseInTheCurrentMonthAddsItAndClearsFilters() {
        initializeEmptyMonth();
        final Expense expense = expense(null, FOOD, "2026-11-04", 10D);
        when(expenseDao.insert(any(ExpenseEntity.class))).thenReturn(42L);

        final ExpenseMainUpdate update = service.saveExpense(expense);

        assertEquals(ADD, update.operationType());
        assertTrue(update.clearFilters());
        assertEquals(expense, update.expense());
        verify(expenseDao).insert(any(ExpenseEntity.class));
    }

    @Test
    public void savingANewSpecialExpenseInTheCurrentMonthDoesNotAddItToTheList() {
        initializeEmptyMonth();
        final Expense expense = expense(null, SPECIAL, "2026-11-04", 10D);

        final ExpenseMainUpdate update = service.saveExpense(expense);

        assertEquals(NONE, update.operationType());
        assertTrue(update.clearFilters());
    }

    @Test
    public void savingANewExpenseInAnotherMonthDoesNotChangeTheListOrFilters() {
        initializeEmptyMonth();
        final Expense expense = expense(null, FOOD, "2026-10-31", 10D);

        final ExpenseMainUpdate update = service.saveExpense(expense);

        assertEquals(NONE, update.operationType());
        assertFalse(update.clearFilters());
    }

    @Test
    public void updatingAnExpenseChoosesUpdateOrRemovalBasedOnItsMonthAndType() {
        initializeEmptyMonth();
        final Expense currentMonthExpense = expense(1L, FOOD, "2026-11-04", 10D);
        final Expense anotherMonthExpense = expense(2L, FOOD, "2026-12-04", 20D);
        final Expense specialExpense = expense(3L, SPECIAL, "2026-11-04", 30D);
        when(expenseDao.update(any(ExpenseEntity.class))).thenReturn(1);

        final ExpenseMainUpdate currentMonthUpdate = service.saveExpense(currentMonthExpense);
        final ExpenseMainUpdate anotherMonthUpdate = service.saveExpense(anotherMonthExpense);
        final ExpenseMainUpdate specialUpdate = service.saveExpense(specialExpense);

        assertEquals(UPDATE, currentMonthUpdate.operationType());
        assertEquals(REMOVE, anotherMonthUpdate.operationType());
        assertEquals(REMOVE, specialUpdate.operationType());
        verify(expenseDao, times(3)).update(any(ExpenseEntity.class));
    }

    @Test
    public void failedPersistenceDoesNotReturnAUiUpdate() {
        initializeEmptyMonth();
        final Expense expense = expense(1L, FOOD, "2026-11-04", 10D);
        doThrow(new IllegalStateException("update failed")).when(expenseDao).update(any(ExpenseEntity.class));

        assertThrows(IllegalStateException.class, () -> service.saveExpense(expense));
    }

    @Test
    public void reconcileChangesClassifiesUpdatesEnteringAndLeavingTheCurrentList() {
        initializeEmptyMonth();
        final Expense updated = expense(1L, FOOD, "2026-11-05", 11D);
        final Expense movedOut = expense(2L, PETROL, "2026-12-05", 20D);
        final Expense movedIn = expense(3L, FOOD, "2026-11-06", 30D);
        final Expense stillOutside = expense(4L, FOOD, "2026-10-06", 40D);

        final List<ExpenseMainUpdate> updates = service.reconcileChanges(
                List.of(
                        expense(1L, FOOD, "2026-11-04", 10D),
                        expense(2L, PETROL, "2026-11-04", 20D)
                ),
                List.of(updated, movedOut, movedIn, stillOutside)
        );

        assertEquals(List.of(UPDATE, REMOVE, ADD, NONE), updates.stream()
                .map(ExpenseMainUpdate::operationType)
                .toList());
        assertEquals(List.of(false, false, true, false), updates.stream()
                .map(ExpenseMainUpdate::clearFilters)
                .toList());
    }

    @Test
    public void reconcileChangesRemovesAnExpenseThatBecomesSpecial() {
        initializeEmptyMonth();
        final Expense specialExpense = expense(1L, SPECIAL, "2026-11-04", 10D);

        final List<ExpenseMainUpdate> updates = service.reconcileChanges(
                List.of(expense(1L, FOOD, "2026-11-04", 10D)),
                List.of(specialExpense)
        );

        assertEquals(REMOVE, updates.getFirst().operationType());
    }

    @Test
    public void totalsByExpenseTypeAggregatesValuesAndHandlesAnEmptyList() {
        final Map<?, Double> totals = service.totalsByExpenseType(List.of(
                expense(1L, FOOD, "2026-11-04", 10D),
                expense(2L, FOOD, "2026-11-05", 5D),
                expense(3L, PETROL, "2026-11-06", 20D)
        ));

        assertEquals(Map.of(FOOD, 15D, PETROL, 20D), totals);
        assertTrue(service.totalsByExpenseType(List.of()).isEmpty());
        assertThrows(UnsupportedOperationException.class, totals::clear);
    }

    @Test
    public void exportDatabaseLoadsAllExpensesAndPublishesProgress() throws IOException {
        final List<Expense> expenses = List.of(
                expense(1L, FOOD, "2026-11-04", 10D),
                expense(2L, PETROL, "2026-11-05", 20D)
        );
        when(expenseDao.fetchAllExpenses()).thenReturn(expenses.stream().map(this::entity).toList());
        final List<DatabaseTransferProgress> progress = new ArrayList<>();

        try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class)) {
            fileUtil.when(() -> FileUtil.exportExpenses(
                            eq(context),
                            eq(expenses),
                            any(IntConsumer.class)
                    ))
                    .thenAnswer(invocation -> {
                        final IntConsumer progressConsumer = invocation.getArgument(2);
                        progressConsumer.accept(1);
                        progressConsumer.accept(2);
                        return null;
                    });

            service.dumpDatabase(progress::add);

            fileUtil.verify(() -> FileUtil.exportExpenses(
                    eq(context),
                    eq(expenses),
                    any(IntConsumer.class)
            ));
        }
        assertEquals(List.of(
                new DatabaseTransferProgress(0, 2, false),
                new DatabaseTransferProgress(1, 2, false),
                new DatabaseTransferProgress(2, 2, false),
                new DatabaseTransferProgress(2, 2, false)
        ), progress);
    }

    @Test
    public void restoreDatabaseImportsAndReplacesExpensesWhilePublishingProgress() throws IOException {
        final Uri backupFile = mock(Uri.class);
        final List<Expense> expenses = List.of(expense(1L, FOOD, "2026-11-04", 10D));
        doAnswer(invocation -> {
            final IntConsumer progress = invocation.getArgument(1);
            progress.accept(1);
            return null;
        }).when(expenseDao).replaceAll(anyList(), any());
        final List<DatabaseTransferProgress> progress = new ArrayList<>();

        try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class)) {
            fileUtil.when(() -> FileUtil.importExpenses(
                    context,
                    backupFile
            )).thenReturn(expenses);

            service.restoreDatabase(backupFile, progress::add);

            fileUtil.verify(() -> FileUtil.importExpenses(
                    context,
                    backupFile
            ));
        }

        verify(expenseDao).replaceAll(anyList(), any());
        assertEquals(List.of(
                new DatabaseTransferProgress(0, 1, true),
                new DatabaseTransferProgress(1, 1, true),
                new DatabaseTransferProgress(1, 1, true)
        ), progress);
    }

    @Test
    public void failedImportDoesNotModifyTheDatabase() throws IOException {
        final Uri backupFile = mock(Uri.class);

        try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class)) {
            fileUtil.when(() -> FileUtil.importExpenses(
                    context,
                    backupFile
            )).thenThrow(new IOException("invalid backup"));

            assertThrows(IOException.class, () -> service.restoreDatabase(backupFile, ignored -> { }));
        }

        verify(expenseDao, never()).replaceAll(anyList(), any());
    }

    private void initializeEmptyMonth() {
        when(expenseDao.fetchExpensesInMonth(SPECIAL.getDbId(), CURRENT_MONTH.toString())).thenReturn(new ArrayList<>());
        when(expenseDao.insert(any(ExpenseEntity.class))).thenReturn(42L);
        service.initialize();
    }

    private Expense expense(final Long id,
                            final ExpenseType expenseType,
                            final String date,
                            final double value) {
        final Expense expense = new Expense();
        expense.setId(id);
        expense.setExpenseType(expenseType);
        expense.setDate(LocalDate.parse(date));
        expense.setValue(value);
        expense.setComment("");
        return expense;
    }

    private ExpenseEntity entity(final Expense expense) {
        return ExpenseEntityMapper.toEntity(expense, true);
    }
}
