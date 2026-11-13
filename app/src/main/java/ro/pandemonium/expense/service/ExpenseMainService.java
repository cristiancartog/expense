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
import java.math.BigDecimal;
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

    public List<ExpenseMainUpdate> reconcileChanges(final List<Expense> currentExpenses, final List<Expense> changedExpenses, final List<Long> deletedExpenseIds) {
        requireInitialized();
        requireNonNull(currentExpenses);
        requireNonNull(changedExpenses);
        requireNonNull(deletedExpenseIds);

        final Set<Long> deletedIds = new HashSet<>(deletedExpenseIds);
        final Set<Long> currentExpenseIds = new HashSet<>();
        currentExpenses.forEach(expense -> currentExpenseIds.add(requireNonNull(expense.getId())));
        final List<ExpenseMainUpdate> updates = new ArrayList<>();
        for (Expense expense : changedExpenses) {
            final Long expenseId = requireNonNull(expense.getId());
            if (deletedIds.contains(expenseId)) {
                continue;
            }
            final boolean alreadyInCurrentList = currentExpenseIds.contains(expenseId);
            final ExpenseMainUpdate.OperationType operationType = operationType(expense, alreadyInCurrentList);
            if (operationType == ADD) {
                currentExpenseIds.add(expenseId);
            } else if (operationType == REMOVE) {
                currentExpenseIds.remove(expenseId);
            }
            updates.add(new ExpenseMainUpdate(expense, operationType, operationType == ADD));
        }
        for (Expense expense : currentExpenses) {
            if (deletedIds.contains(expense.getId())) {
                updates.add(new ExpenseMainUpdate(expense, REMOVE, false));
            }
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

    public Map<ExpenseType, BigDecimal> totalsByExpenseType(final List<Expense> expenses) {
        requireNonNull(expenses);

        final Map<ExpenseType, BigDecimal> totals = new EnumMap<>(ExpenseType.class);
        totals.putAll(ExpenseUtil.computeExpenseSumByCategory(expenses));
        return Collections.unmodifiableMap(totals);
    }

    public boolean dumpDatabase(final Consumer<DatabaseTransferProgress> progressPublisher) throws IOException {
        requireNonNull(progressPublisher);

        final List<Expense> expenses = ExpenseEntityMapper.toExpenses(expenseDao.fetchAllExpenses());
        if (expenses.isEmpty()) {
            return false;
        }

        final ProgressPublisher publisher = new ProgressPublisher(expenses.size(), false, progressPublisher);
        publisher.publish(0, true);
        FileUtil.exportExpenses(context, expenses, publisher::publishIntermediate);
        publisher.publish(expenses.size(), true);
        return true;
    }

    public List<Expense> previewDatabaseRestore(final Uri backupFile) throws IOException {
        requireNonNull(backupFile);

        return FileUtil.importExpenses(context, backupFile);
    }

    public void restoreDatabase(final List<Expense> expenses, final Consumer<DatabaseTransferProgress> progressPublisher) throws IOException {
        requireNonNull(expenses);
        requireNonNull(progressPublisher);
        if (expenses.isEmpty()) {
            throw new IOException("Backup contains 0 expenses. Empty backups cannot be restored.");
        }

        final List<ExpenseEntity> entities = prepareRestoreEntities(expenses);
        dumpDatabase(progressPublisher);
        final ProgressPublisher publisher = new ProgressPublisher(entities.size(), true, progressPublisher);
        publisher.publish(0, true);
        expenseDao.replaceAll(entities, publisher::publishIntermediate);
        publisher.publish(entities.size(), true);
    }

    private List<ExpenseEntity> prepareRestoreEntities(final List<Expense> expenses) throws IOException {
        try {
            return expenses.stream()
                           .peek(this::validateRestoreExpense)
                           .map(expense -> ExpenseEntityMapper.toEntity(expense, false))
                           .toList();
        } catch (IllegalArgumentException e) {
            throw new IOException("Backup contains an invalid expense", e);
        }
    }

    private void validateRestoreExpense(final Expense expense) {
        if (expense == null || expense.getExpenseType() == null || expense.getDate() == null || expense.getComment() == null) {
            throw new IllegalArgumentException("Expense is missing required fields");
        }

        if (!ExpenseUtil.isValidValue(expense.getValue())) {
            throw new IllegalArgumentException("Expense contains an invalid amount");
        }
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
