package ro.pandemonium.expense.db;

import static java.util.Objects.requireNonNull;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

@Dao
public abstract class ExpenseDao {

    @Insert
    public abstract long insert(ExpenseEntity expense);

    @Update
    public abstract int update(ExpenseEntity expense);

    @Query("""
        DELETE
        FROM EXPENSES
        WHERE _ID = :expenseId
        """)
    public abstract int delete(long expenseId);

    @Query("""
        DELETE
        FROM EXPENSES
        """)
    protected abstract void deleteAll();

    @Transaction
    public List<ExpenseEntity> fetchExpenses(final List<Integer> expenseTypeIds, final Set<String> comments) {
        requireNonNull(expenseTypeIds);
        requireNonNull(comments);

        final Map<Long, ExpenseEntity> expensesById = new LinkedHashMap<>();
        if (!expenseTypeIds.isEmpty()) {
            fetchByExpenseTypes(expenseTypeIds).forEach(expense -> expensesById.put(expense.id, expense));
        }

        for (String comment : comments) {
            final String commentPattern = "%" + escapeLikeValue(comment) + "%";
            fetchByComment(commentPattern).forEach(expense -> expensesById.put(expense.id, expense));
        }

        return expensesById.values().stream()
                           .sorted(Comparator.comparing((ExpenseEntity expense) -> expense.date)
                                             .reversed())
                           .toList();
    }

    @Query("""
        SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT
        FROM EXPENSES
        WHERE EXPENSE_TYPE IN (:expenseTypeIds)
        """)
    protected abstract List<ExpenseEntity> fetchByExpenseTypes(List<Integer> expenseTypeIds);

    @Query("""
        SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT
        FROM EXPENSES
        WHERE COMMENT LIKE :commentPattern ESCAPE '\'
        """)
    protected abstract List<ExpenseEntity> fetchByComment(String commentPattern);

    private String escapeLikeValue(final String value) {
        return value.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }

    @Query("""
        SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT
        FROM EXPENSES
        WHERE EXPENSE_TYPE <> :excludedExpenseType
            AND STRFTIME('%Y-%m', DATE) = :yearMonth
        """)
    public abstract List<ExpenseEntity> fetchExpensesInMonth(int excludedExpenseType, String yearMonth);

    @Query("""
        SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT
        FROM EXPENSES
        """)
    public abstract List<ExpenseEntity> fetchAllExpenses();

    @Query("""
        SELECT SUM(VALUE) AS TOTAL, STRFTIME('%m', DATE) AS MONTH
        FROM EXPENSES
        WHERE EXPENSE_TYPE = :expenseType AND STRFTIME('%Y', DATE) = :year
        GROUP BY MONTH
        ORDER BY MONTH
        """)
    public abstract List<YearSummaryRow> getMonthlySummaryInYear(int expenseType, String year);

    @Query("""
        SELECT EXPENSE_TYPE, SUM(VALUE) AS TOTAL, STRFTIME('%Y-%m', DATE) AS MONTH
        FROM EXPENSES
        WHERE EXPENSE_TYPE IN (:expenseTypes)
        GROUP BY EXPENSE_TYPE, MONTH
        ORDER BY MONTH
        """)
    public abstract List<MonthlySummaryRow> getMonthlySummary(List<Integer> expenseTypes);

    @Query("""
        SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT
        FROM EXPENSES
        ORDER BY DATE ASC
        LIMIT 1
        """)
    public abstract ExpenseEntity getEarliestEntry();

    @Query("""
        SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT
        FROM EXPENSES
        ORDER BY DATE DESC
        LIMIT 1
        """)
    public abstract ExpenseEntity getLatestEntry();

    @Query("""
        SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT
        FROM EXPENSES
        WHERE (:startInclusive IS NULL OR DATE >= :startInclusive)
            AND (:endInclusive IS NULL OR DATE <= :endInclusive)
        """)
    public abstract List<ExpenseEntity> findExpensesInInterval(LocalDate startInclusive, LocalDate endInclusive);

    @Transaction
    public void replaceAll(final List<ExpenseEntity> expenses, final IntConsumer progressConsumer) {
        requireNonNull(expenses);
        requireNonNull(progressConsumer);

        deleteAll();

        int restoredCount = 0;
        for (ExpenseEntity expense : expenses) {
            expense.id = insert(expense);
            progressConsumer.accept(++restoredCount);
        }
    }
}
