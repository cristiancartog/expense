package ro.pandemonium.expense;

import android.app.Application;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.ExpenseOpenHelper;

public class ExpenseApplication extends Application {

    private ExpenseDao expenseDao;

    @Override
    public void onCreate() {
        super.onCreate();

        final ExpenseOpenHelper openHelper = new ExpenseOpenHelper(this);
        expenseDao = new ExpenseDao(openHelper.getWritableDatabase());
    }

    public ExpenseDao getExpenseDao() {
        return expenseDao;
    }
}
