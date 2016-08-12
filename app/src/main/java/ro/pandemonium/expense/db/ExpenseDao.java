package ro.pandemonium.expense.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseMonthlySummary;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ExpenseDao implements Serializable {

    private static final String EXPENSES_TABLE = "EXPENSES";
    private static final String FETCH_EXPENSES_QUERY_BASE = "SELECT _ID, EXPENSE_TYPE, VALUE, DATE, COMMENT FROM " + EXPENSES_TABLE;

    private SQLiteDatabase database;
    private SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DB, Locale.getDefault());

    public ExpenseDao(SQLiteDatabase database) {
        this.database = database;
    }

    private void checkDatabaseAvailability() {
        if (database == null) {
            final String msg = "No database handle!";
            Log.e(Constants.APPLICATION_NAME, msg);
            throw new RuntimeException(msg);
        }
        if (!database.isOpen()) {

        }
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
        cv.put("DATE", dateFormat.format(expense.getDate()));
        cv.put("COMMENT", expense.getComment());
        return cv;
    }

    public void removeExpense(final Long expenseId) {
        database.execSQL("DELETE FROM EXPENSES WHERE _ID = " + expenseId);
    }

    public List<Expense> fetchExpenses(final List<ExpenseType> expenseTypes) {
        checkDatabaseAvailability();

        final Cursor cursor = database.rawQuery(createFetchExpensesQueryString(expenseTypes), null);
        return extractExpenses(cursor);
    }

    public List<Expense> fetchExpenses(final Filters filters) {
        checkDatabaseAvailability();

        final Cursor cursor = database.rawQuery(createFetchExpensesQueryString(filters), null);
        return extractExpenses(cursor);
    }

    private String createFetchExpensesQueryString(final Filters filters) {
        final StringBuilder query = new StringBuilder(FETCH_EXPENSES_QUERY_BASE);

        query.append(createExpenseTypeWhereClause(filters.getExpenseTypes()));
        query.append(buildDescriptionClause(filters.getComments()));

        return query.toString();
    }

    private String createFetchExpensesQueryString(final List<ExpenseType> expenseTypes) {
        final StringBuilder query = new StringBuilder(FETCH_EXPENSES_QUERY_BASE);

        query.append(createExpenseTypeWhereClause(expenseTypes));

        return query.toString();
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
        final StringBuilder sb = new StringBuilder(" WHERE EXPENSE_TYPE IN (");

        final int expenseTypeListSize = expenseTypes.size();
        for (int i = 0; i < expenseTypeListSize; i++) {
            sb.append(expenseTypes.get(i).getDbId());
            if (i < expenseTypeListSize - 1) {
                sb.append(",");
            }
        }

        sb.append(")");

        return sb.toString();
    }

    public List<Expense> fetchExpenses(final Integer year, final Integer monthOfYear) {
        checkDatabaseAvailability();

        final Cursor cursor = database.rawQuery(createFetchExpensesQueryString(year, monthOfYear), null);
        return extractExpenses(cursor);
    }

    private String createFetchExpensesQueryString(final Integer year, final Integer monthOfYear) {
        String query = FETCH_EXPENSES_QUERY_BASE;

        if (monthOfYear != null) {
            query += " WHERE CAST(STRFTIME('%Y', DATE) AS NUMBER) = " + year + " AND CAST(STRFTIME('%m', DATE) AS NUMBER) = " + monthOfYear;
        }

        return query;
    }

    private List<Expense> extractExpenses(final Cursor cursor) {
        final List<Expense> expenses = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                final Expense expense = new Expense();
                expense.setId(cursor.getLong(0));
                expense.setExpenseType(ExpenseType.forDbId(cursor.getInt(1)));
                expense.setValue(cursor.getDouble(2));
                try {
                    expense.setDate(dateFormat.parse(cursor.getString(3)));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                expense.setComment(cursor.getString(4));

                expenses.add(expense);
            } while (cursor.moveToNext());
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }

        return expenses;
    }

    public List<ExpenseMonthlySummary> getMonthlySummary2(final List<ExpenseType> expenseTypes) {
        final List<ExpenseMonthlySummary> monthlySummary = new ArrayList<>();

        checkDatabaseAvailability();

        final StringBuilder querySb = new StringBuilder("SELECT EXPENSE_TYPE, SUM(VALUE), STRFTIME('%Y-%m', DATE) AS MONTH FROM EXPENSES ");
        querySb.append(createExpenseTypeWhereClause(expenseTypes));
        querySb.append("GROUP BY EXPENSE_TYPE, MONTH ");
        querySb.append("ORDER BY MONTH");

        ExpenseMonthlySummary currentMonthlySummary = null;

        final Cursor cursor = database.rawQuery(querySb.toString(), null);
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
                currentMonthlySummary.addExpense(ExpenseType.forDbId(cursor.getInt(0)), cursor.getDouble(1));

            } while (cursor.moveToNext());

            monthlySummary.add(currentMonthlySummary);
        }

        return monthlySummary;
    }

    public void restoreExpenses(final List<Expense> expenses) {
        database.execSQL("DELETE FROM " + EXPENSES_TABLE);

        for (Expense expense : expenses) {
            persistExpense(expense);
        }
    }

    public void closeDatabase() {
        database.close();
    }
}
