package ro.pandemonium.expense;

import android.app.Application;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.ExpenseOpenHelper;

public class ExpenseApplication extends Application {

    private ExpenseDao expenseDao;

    @Override
    public void onCreate() {
        super.onCreate();

        //noinspection resource -- this SQLiteOpenHelper is application scoped and its lifetime is managed by Android
        final ExpenseOpenHelper openHelper = new ExpenseOpenHelper(this);
        expenseDao = new ExpenseDao(openHelper.getWritableDatabase());
    }

    public ExpenseDao getExpenseDao() {
        return expenseDao;
    }
}
