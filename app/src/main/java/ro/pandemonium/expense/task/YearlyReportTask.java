package ro.pandemonium.expense.task;

import android.os.AsyncTask;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.YearlyReportParams;
import ro.pandemonium.expense.model.YearlyReportResult;

import static ro.pandemonium.expense.Constants.LEADING_ZERO_FORMAT;

public class YearlyReportTask extends AsyncTask<YearlyReportParams, Void, YearlyReportResult> {

    public interface YearlyReportTaskCallback {
        void reportFinished(YearlyReportResult report);
    }

    private ExpenseDao expenseDao;
    private YearlyReportTaskCallback callback;
    private final NumberFormat numberFormatter = new DecimalFormat(LEADING_ZERO_FORMAT);

    public YearlyReportTask(final ExpenseDao expenseDao, final YearlyReportTaskCallback callback) {
        this.expenseDao = expenseDao;
        this.callback = callback;
    }

    @Override
    protected YearlyReportResult doInBackground(final YearlyReportParams... params) {
        YearlyReportParams yearlyReportParams = params[0];

        Map<String, Double> currentYearExpenses = expenseDao.getMonthlySummaryInYear(yearlyReportParams.getExpenseType(),
                yearlyReportParams.getYear());
        Map<String, Double> lastYearExpenses = expenseDao.getMonthlySummaryInYear(yearlyReportParams.getExpenseType(),
                yearlyReportParams.getYear() - 1);

        fillMissingValues(currentYearExpenses);
        fillMissingValues(lastYearExpenses);

        return new YearlyReportResult(currentYearExpenses, lastYearExpenses);
    }

    private void fillMissingValues(final Map<String, Double> monthlyExpenses) {
        for (int i = 1; i <= 12; i++) {
            String month = numberFormatter.format(i);

            if (!monthlyExpenses.containsKey(month)) {
                monthlyExpenses.put(month, 0D);
            }
        }
    }

    @Override
    protected void onPostExecute(final YearlyReportResult result) {
        super.onPostExecute(result);
        callback.reportFinished(result);

        this.callback = null;
        this.expenseDao = null;
    }
}
