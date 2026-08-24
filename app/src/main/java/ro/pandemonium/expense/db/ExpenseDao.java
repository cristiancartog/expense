package ro.pandemonium.expense.db;

import static java.util.stream.Collectors.joining;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_DB;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseMonthlySummary;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;

public class ExpenseDao implements Serializable {

    private static final String CLASS_NAME = ExpenseDao.class.getSimpleName();
    private static final String EXPENSES_TABLE = "EXPENSES";
    private static final String FETCH_EXPENSES_QUERY_BASE = """
        SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT
        FROM EXPENSES
    """;

    private final SQLiteDatabase database;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_DB, Locale.getDefault());

    public ExpenseDao(final SQLiteDatabase database) {
        this.database = database;
    }

    private void checkDatabaseAvailability() {
        if (database == null) {
            final String msg = "No database handle!";
            Log.e(Constants.APPLICATION_NAME, msg);
            throw new ExpenseDaoException(msg);
        }
//        if (!database.isOpen()) {
//
//        }
    }

    public void persistExpense(final Expense expense) {
        checkDatabaseAvailability();

        final ContentValues cv = createContentValuesFromExpense(expense);

        final long id = database.insert(EXPENSES_TABLE, null, cv);
        if (id > 0) {
            expense.setId(id);
        } else {
            throw new ExpenseDaoException("expense not inserted");
        }
    }

    public void updateExpense(final Expense expense) {
        checkDatabaseAvailability();

        final ContentValues cv = createContentValuesFromExpense(expense);

        final int updatedCount = database.update(EXPENSES_TABLE, cv, "_ID = " + expense.getId(), null);
        if (updatedCount != 1) {
            throw new ExpenseDaoException("Expense with id " + expense.getId() + " was not updated");
        }
    }

    private ContentValues createContentValuesFromExpense(final Expense expense) {
        final ContentValues cv = new ContentValues();
        cv.put("EXPENSE_TYPE", expense.getExpenseType().getDbId());
        cv.put("VALUE", expense.getValue());
        cv.put("DATE", dateFormatter.format(expense.getDate()));
        cv.put("COMMENT", expense.getComment());
        return cv;
    }

    public void removeExpense(final Long expenseId) {
        database.execSQL("DELETE FROM EXPENSES WHERE _ID = " + expenseId);
    }

    public List<Expense> fetchExpenses(final Filters filters) {
        checkDatabaseAvailability();

        final Cursor cursor = database.rawQuery(createFetchExpensesQueryString(filters), null);
        return extractExpenses(cursor);
    }

    private String createFetchExpensesQueryString(final Filters filters) {
        return FETCH_EXPENSES_QUERY_BASE + createExpenseTypeWhereClause(filters.expenseTypes()) + buildDescriptionClause(filters.comments());
    }

    private String buildDescriptionClause(final Set<String> comments) {
        final StringBuilder sb = new StringBuilder();

        for (String comment : comments) {
            sb.append(" OR COMMENT LIKE '%");
            sb.append(comment);
            sb.append("%'");
        }

        return sb.toString();
    }

    private String createExpenseTypeWhereClause(final List<ExpenseType> expenseTypes) {
        String expenseTypesInArgs = expenseTypes.stream()
                .map(ExpenseType::getDbId)
                .map(String::valueOf)
                .collect(joining(","));

        return String.format("EXPENSE_TYPE IN (%s)", expenseTypesInArgs);
    }

    public List<Expense> fetchExpenses(final Integer year, final Month month) {
        return queryForExpenses(createFetchExpensesQueryString(year, month));
    }

    private String createFetchExpensesQueryString(final Integer year, final Month month) {
        String query = FETCH_EXPENSES_QUERY_BASE;

        if (month != null) {
            query += " WHERE "
                    + "EXPENSE_TYPE <> " + ExpenseType.SPECIAL.getDbId()
                    + " AND CAST(STRFTIME('%Y', DATE) AS NUMBER) = " + year
                    + " AND CAST(STRFTIME('%m', DATE) AS NUMBER) = " + month.getValue();
        }

        return query;
    }

    public Map<String, Double> getMonthlySummaryInYear(final ExpenseType expenseType, final int year) {
        String query = "SELECT SUM(VALUE), STRFTIME('%m', DATE) AS MONTH FROM EXPENSES ";
        query += "WHERE EXPENSE_TYPE = " + expenseType.getDbId();
        query += "  AND CAST(STRFTIME('%Y', DATE) AS NUMBER) = " + year;
        query += " GROUP BY MONTH ";
        query += " ORDER BY MONTH";

        Map<String, Double> summary = new TreeMap<>();

        try (final Cursor cursor = database.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    String yearMonth = cursor.getString(1);
                    Double value = cursor.getDouble(0);

                    summary.put(yearMonth, value);
                } while (cursor.moveToNext());
            }
        }

        return summary;
    }

    public List<ExpenseMonthlySummary> getMonthlySummary(final List<ExpenseType> expenseTypes) {
        final List<ExpenseMonthlySummary> monthlySummary = new ArrayList<>();

        checkDatabaseAvailability();

        String query = "SELECT EXPENSE_TYPE, SUM(VALUE), STRFTIME('%Y-%m', DATE) AS MONTH FROM EXPENSES ";
        query += createExpenseTypeWhereClause(expenseTypes);
        query += "GROUP BY EXPENSE_TYPE, MONTH ";
        query += "ORDER BY MONTH";

        ExpenseMonthlySummary currentMonthlySummary = null;

        try (final Cursor cursor = database.rawQuery(query, null)) {
            if (cursor.moveToFirst()) {
                do {
                    String yearMonth = cursor.getString(2);
                    if (currentMonthlySummary == null) {
                        currentMonthlySummary = new ExpenseMonthlySummary(yearMonth);
                    }

                    if (!currentMonthlySummary.isInSameMonth(yearMonth)) {
                        monthlySummary.add(currentMonthlySummary);
                        currentMonthlySummary = new ExpenseMonthlySummary(yearMonth);
                    }
                    currentMonthlySummary.addExpense(ExpenseType.byDbId(cursor.getInt(0)), cursor.getDouble(1));

                } while (cursor.moveToNext());

                monthlySummary.add(currentMonthlySummary);
            }
        }

        return monthlySummary;
    }

    public Expense getEarliestEntry() {
        String query = FETCH_EXPENSES_QUERY_BASE + " ORDER BY DATE ASC LIMIT 1";
        Iterator<Expense> iterator = queryForExpenses(query).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public Expense getLatestEntry() {
        String query = FETCH_EXPENSES_QUERY_BASE + " ORDER BY DATE DESC LIMIT 1";
        Iterator<Expense> iterator = queryForExpenses(query).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    public List<Expense> getExpensesInInterval(final LocalDate start, final LocalDate end) {
        StringBuilder query = new StringBuilder(FETCH_EXPENSES_QUERY_BASE);

        if (start != null || end != null) {
            query.append(" WHERE ");
            if (start != null) {
                query.append(" DATE >= '");
                query.append(dateFormatter.format(start));
                query.append("'");
                if (end != null) {
                    query.append(" AND ");
                }
            }

            if (end != null) {
                query.append(" DATE <= '");
                query.append(dateFormatter.format(end));
                query.append("'");
            }
        }

        return queryForExpenses(query.toString());
    }

    private List<Expense> queryForExpenses(final String query) {
        checkDatabaseAvailability();

        try (Cursor cursor = database.rawQuery(query, null)) {
            return extractExpenses(cursor);
        }
    }

    private List<Expense> extractExpenses(final Cursor cursor) {
        final List<Expense> expenses = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                final Expense expense = new Expense();
                expense.setId(cursor.getLong(0));
                expense.setExpenseType(ExpenseType.byDbId(cursor.getInt(1)));
                expense.setValue(cursor.getDouble(2));
                expense.setDate(extractDate(cursor.getString(3)));
                expense.setComment(cursor.getString(4));

                expenses.add(expense);
            } while (cursor.moveToNext());
        }

        return expenses;
    }

    private LocalDate extractDate(final String dateString) {
        try {
            return LocalDate.parse(dateString, dateFormatter);
        } catch (DateTimeParseException ex) {
            Log.w(CLASS_NAME, ex.getMessage());
            return null;
        }
    }

    public void restoreExpenses(final List<Expense> expenses) {
        database.execSQL("DELETE FROM " + EXPENSES_TABLE);

        expenses.forEach(this::persistExpense);
    }

//    public void closeDatabase() {
//        database.close();
//    }
}
