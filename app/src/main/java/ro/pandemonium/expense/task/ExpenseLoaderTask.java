package ro.pandemonium.expense.task;

import android.os.AsyncTask;

import java.util.List;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.MonthWrapper;

public class ExpenseLoaderTask extends AsyncTask<MonthWrapper, Void, List<Expense>> {

    public interface ExpenseLoaderCallback {
        void expensesLoaded(List<Expense> expenses);
    }

    private ExpenseDao expenseDao;
    private ExpenseLoaderCallback callback;

    public ExpenseLoaderTask(final ExpenseDao expenseDao, final ExpenseLoaderCallback callback) {
        this.expenseDao = expenseDao;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected List<Expense> doInBackground(final MonthWrapper... wrappers) {
        MonthWrapper monthWrapper = wrappers[0];
        return expenseDao.fetchExpenses(monthWrapper.getYear(), monthWrapper.getMonth());
    }

    @Override
    protected void onPostExecute(final List<Expense> expenses) {
        super.onPostExecute(expenses);
        callback.expensesLoaded(expenses);

        this.callback = null;
        this.expenseDao = null;
    }
}
