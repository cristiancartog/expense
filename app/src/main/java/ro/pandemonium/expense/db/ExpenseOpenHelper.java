package ro.pandemonium.expense.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import ro.pandemonium.expense.Constants;

public class ExpenseOpenHelper extends SQLiteOpenHelper {

    private static final int VERSION_NUMBER = 9;
    private static final String DB_NAME = "expenses.db";

    public ExpenseOpenHelper(final Context context) {
        super(context, DB_NAME, null, VERSION_NUMBER);
        Log.i(Constants.APPLICATION_NAME, "create db: " + DB_NAME + " version: " + VERSION_NUMBER);
    }

    @Override
    public void onCreate(final SQLiteDatabase sqLiteDatabase) {
        Log.i(Constants.APPLICATION_NAME, "onCreate()");
        sqLiteDatabase.execSQL(generateExpenseTableSql());
    }

    @Override
    public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int oldVersion, final int newVersion) {
        Log.i(Constants.APPLICATION_NAME, "onUpdate()");

//        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS EXPENSES");

        onCreate(sqLiteDatabase);
    }

    private String generateExpenseTableSql() {
        String query = "CREATE TABLE EXPENSES(";
        query += " _ID INTEGER PRIMARY KEY AUTOINCREMENT,";
        query += " EXPENSE_TYPE INTEGER NOT NULL,";
        query += " VALUE REAL NOT NULL,";
        query += " DATE TEXT NOT NULL,";
        query += " COMMENT TEXT)";

        return query;
    }
}
