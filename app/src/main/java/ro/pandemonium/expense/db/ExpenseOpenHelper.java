package ro.pandemonium.expense.db;

import static ro.pandemonium.expense.Constants.APPLICATION_NAME;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ExpenseOpenHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "expenses.db";

    public ExpenseOpenHelper(final Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        Log.i(APPLICATION_NAME, "create db: " + DB_NAME + " version: " + DB_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase database) {
        Log.i(APPLICATION_NAME, "onCreate()");

        database.execSQL("""
            CREATE TABLE EXPENSES(
                _ID INTEGER PRIMARY KEY AUTOINCREMENT,
                EXPENSE_TYPE INTEGER NOT NULL,
                VALUE REAL NOT NULL,
                DATE TEXT NOT NULL,
                COMMENT TEXT)
            """);

        database.execSQL("""
            CREATE INDEX IF NOT EXISTS
                IDX_EXPENSE_DATE ON EXPENSES(DATE)
            """
        );
    }

    @Override
    public void onUpgrade(final SQLiteDatabase sqLiteDatabase, final int oldVersion, final int newVersion) {
        Log.i(APPLICATION_NAME, "%s onUpgrade(sqliteDatabase, %d, %d)".formatted(sqLiteDatabase.getPath(), oldVersion, newVersion));
    }

}
