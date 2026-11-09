package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;
import static ro.pandemonium.expense.service.ExpenseMainUpdate.OperationType.ADD;
import static ro.pandemonium.expense.service.ExpenseMainUpdate.OperationType.NONE;
import static ro.pandemonium.expense.service.ExpenseMainUpdate.OperationType.REMOVE;
import static ro.pandemonium.expense.service.ExpenseMainUpdate.OperationType.UPDATE;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ActivityScoped;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.ExpenseEntity;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.util.FileUtil;

@ActivityScoped
public final class ExpenseMainService {

    private static final long PROGRESS_UPDATE_INTERVAL_MILLIS = 1_000L;

    private final ExpenseDao expenseDao;
    private final Context context;
    private final Clock clock;
    private final LongSupplier currentTimeMillis;

    private YearMonth yearMonth;

    @Inject
    public ExpenseMainService(final ExpenseDao expenseDao,
                              @ApplicationContext final Context context) {
        this(
            expenseDao,
            context,
            Clock.systemDefaultZone(),
            System::currentTimeMillis
        );
    }

    ExpenseMainService(final ExpenseDao expenseDao,
                       final Context context,
                       final Clock clock,
                       final LongSupplier currentTimeMillis) {
        this.expenseDao = requireNonNull(expenseDao);
        this.context = requireNonNull(context);
        this.clock = requireNonNull(clock);
        this.currentTimeMillis = requireNonNull(currentTimeMillis);
    }

    public ExpenseMainState initialize() {
        return load(YearMonth.now(clock));
    }

    public ExpenseMainState nextMonth() {
        requireInitialized();

        return load(yearMonth.plusMonths(1));
    }

    public ExpenseMainState previousMonth() {
        requireInitialized();

        return load(yearMonth.minusMonths(1));
    }

    public ExpenseMainState selectMonth(final YearMonth selectedMonth) {
        requireInitialized();
        requireNonNull(selectedMonth);

        return load(selectedMonth);
    }

    public ExpenseMainState reload() {
        requireInitialized();

        return load(yearMonth);
    }

    public ExpenseMainUpdate saveExpense(final Expense expense) {
        requireInitialized();
        requireNonNull(expense);

        final boolean alreadyInCurrentList = expense.getId() != null;
        if (alreadyInCurrentList) {
            final int updatedCount = expenseDao.update(ExpenseEntityMapper.toEntity(expense, true));
            if (updatedCount != 1) {
                throw new IllegalStateException("Expense with id " + expense.getId() + " was not updated");
            }
        } else {
            final long id = expenseDao.insert(ExpenseEntityMapper.toEntity(expense, false));
            if (id <= 0) {
                throw new IllegalStateException("Expense was not inserted");
            }
            expense.setId(id);
        }

        final ExpenseMainUpdate.OperationType operationType = operationType(expense, alreadyInCurrentList);
        return new ExpenseMainUpdate(expense, operationType,!alreadyInCurrentList && isInCurrentMonth(expense));
    }

    public List<ExpenseMainUpdate> reconcileChanges(final List<Expense> currentExpenses, final List<Expense> changedExpenses) {
        requireInitialized();
        requireNonNull(currentExpenses);
        requireNonNull(changedExpenses);

        final Set<Long> currentExpenseIds = new HashSet<>();
        currentExpenses.forEach(expense -> currentExpenseIds.add(requireNonNull(expense.getId())));
        final List<ExpenseMainUpdate> updates = new ArrayList<>();
        for (Expense expense : changedExpenses) {
            final Long expenseId = requireNonNull(expense.getId());
            final boolean alreadyInCurrentList = currentExpenseIds.contains(expenseId);
            final ExpenseMainUpdate.OperationType operationType = operationType(expense, alreadyInCurrentList);
            if (operationType == ADD) {
                currentExpenseIds.add(expenseId);
            } else if (operationType == REMOVE) {
                currentExpenseIds.remove(expenseId);
            }
            updates.add(new ExpenseMainUpdate(expense, operationType, operationType == ADD));
        }
        return List.copyOf(updates);
    }

    private ExpenseMainUpdate.OperationType operationType(final Expense expense, final boolean alreadyInCurrentList) {
        final boolean belongsInCurrentList = isInCurrentMonth(expense) && expense.getExpenseType() != SPECIAL;
        if (belongsInCurrentList) {
            return alreadyInCurrentList ? UPDATE : ADD;
        }
        return alreadyInCurrentList ? REMOVE : NONE;
    }

    private boolean isInCurrentMonth(final Expense expense) {
        return YearMonth.from(expense.getDate())
                        .equals(yearMonth);
    }

    private ExpenseMainState load(final YearMonth requestedMonth) {
        final List<ExpenseEntity> entities = expenseDao.fetchExpensesInMonth(SPECIAL.getDbId(), requestedMonth.toString());
        final List<Expense> expenses = ExpenseEntityMapper.toExpenses(entities).stream()
                                                                 .sorted(Comparator.comparing(Expense::getDate))
                                                                 .toList();
        yearMonth = requestedMonth;
        return new ExpenseMainState(yearMonth, expenses);
    }

    public Map<ExpenseType, Double> totalsByExpenseType(final List<Expense> expenses) {
        requireNonNull(expenses);

        final Map<ExpenseType, Double> totals = new EnumMap<>(ExpenseType.class);
        totals.putAll(ExpenseUtil.computeExpenseSumByCategory(expenses));
        return Collections.unmodifiableMap(totals);
    }

    public void dumpDatabase(final Consumer<DatabaseTransferProgress> progressPublisher) throws IOException {
        requireNonNull(progressPublisher);

        final List<Expense> expenses = ExpenseEntityMapper.toExpenses(expenseDao.fetchAllExpenses());
        final ProgressPublisher publisher = new ProgressPublisher(expenses.size(), false, progressPublisher);
        publisher.publish(0, true);
        FileUtil.exportExpenses(context, expenses, publisher::publishIntermediate);
        publisher.publish(expenses.size(), true);
    }

    public void restoreDatabase(final Uri backupFile, final Consumer<DatabaseTransferProgress> progressPublisher) throws IOException {
        requireNonNull(backupFile);
        requireNonNull(progressPublisher);

        final List<Expense> expenses = FileUtil.importExpenses(context, backupFile);
        final List<ExpenseEntity> entities = expenses.stream()
                                                     .map(expense -> ExpenseEntityMapper.toEntity(expense, false))
                                                     .toList();
        final ProgressPublisher publisher = new ProgressPublisher(expenses.size(), true, progressPublisher);
        publisher.publish(0, true);
        expenseDao.replaceAll(entities, publisher::publishIntermediate);
        for (int i = 0; i < expenses.size(); i++) {
            expenses.get(i).setId(entities.get(i).id);
        }
        publisher.publish(expenses.size(), true);
    }

    private void requireInitialized() {
        if (yearMonth == null) {
            throw new IllegalStateException("Expense main service has not been initialized");
        }
    }

    private final class ProgressPublisher {

        private final int total;
        private final boolean restore;
        private final Consumer<DatabaseTransferProgress> publisher;
        private long lastPublishedAt;

        private ProgressPublisher(final int total,
                                  final boolean restore,
                                  final Consumer<DatabaseTransferProgress> publisher) {
            this.total = total;
            this.restore = restore;
            this.publisher = publisher;
        }

        private void publishIntermediate(final int completed) {
            publish(completed, false);
        }

        private void publish(final int completed, final boolean force) {
            final long now = currentTimeMillis.getAsLong();
            if (force || now - lastPublishedAt >= PROGRESS_UPDATE_INTERVAL_MILLIS) {
                lastPublishedAt = now;
                publisher.accept(new DatabaseTransferProgress(completed, total, restore));
            }
        }
    }
}
