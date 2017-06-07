package ro.pandemonium.expense.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

import static ro.pandemonium.expense.Constants.INTENT_YEAR;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;

public class YearlyExpenseReportActivity extends Activity implements View.OnClickListener {

    private final NumberFormat numberFormatter = new DecimalFormat(NUMBER_FORMAT_PATTERN);

    private ExpenseDao expenseDao;

    private TextView currentYearWithSpecialText;
    private TextView lastYearWithSpecialText;
    private TextView currentYearWithoutSpecialText;
    private TextView lastYearWithoutSpecialText;
    private TextView withSpecialVariationText;
    private TextView withoutSpecialVariationText;

    private int year;
    private Map<ExpenseType, TextView> currentYearSumTexts = new HashMap<>();
    private Map<ExpenseType, TextView> lastYearSumTexts = new HashMap<>();
    private Map<ExpenseType, TextView> variationTexts = new HashMap<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.yearly_report_activity);

        currentYearWithSpecialText = (TextView) findViewById(R.id.yearlyReportCurrentYearWithSpecial);
        lastYearWithSpecialText = (TextView) findViewById(R.id.yearlyReportLastYearWithSpecial);
        currentYearWithoutSpecialText = (TextView) findViewById(R.id.yearlyReportCurrentYearWithoutSpecial);
        lastYearWithoutSpecialText = (TextView) findViewById(R.id.yearlyReportLastYearWithoutSpecial);
        withSpecialVariationText = (TextView) findViewById(R.id.yearlyReportWithSpecialVariation);
        withoutSpecialVariationText = (TextView) findViewById(R.id.yearlyReportWithoutSpecialVariation);
        Button nextYearButton = (Button) findViewById(R.id.yearlyReportNextYear);
        Button previousYearButton = (Button) findViewById(R.id.yearlyReportPreviousYear);

        nextYearButton.setOnClickListener(this);
        previousYearButton.setOnClickListener(this);

        expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();

        year = getIntent().getExtras().getInt(INTENT_YEAR);

        TableLayout byExpenseTypeLayout = (TableLayout) findViewById(R.id.yearlyReportByExpenseTypeLayout);

        for (ExpenseType expenseType : ExpenseType.values()) {
            TableRow tableRow = new TableRow(this);

            TextView expenseTypeLabel = new TextView(this);
            expenseTypeLabel.setText(expenseType.getTextResource());
            tableRow.addView(expenseTypeLabel);

            TextView currentYear = valueTextView();
            tableRow.addView(currentYear);
            currentYearSumTexts.put(expenseType, currentYear);

            TextView lastYear = valueTextView();
            tableRow.addView(lastYear);
            lastYearSumTexts.put(expenseType, lastYear);

            TextView variation = valueTextView();
            tableRow.addView(variation);
            variationTexts.put(expenseType, variation);

            byExpenseTypeLayout.addView(tableRow);
        }

        recomputeReport();
    }

    private TextView valueTextView() {
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.RIGHT);
        return textView;
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.yearlyReportNextYear:
                year++;
                recomputeReport();
                break;
            case R.id.yearlyReportPreviousYear:
                year--;
                recomputeReport();
                break;
        }
    }

    private void recomputeReport() {
        List<Expense> currentYearExpenses = expenseDao.getExpensesInYear(year);
        List<Expense> lastYearExpenses = expenseDao.getExpensesInYear(year - 1);

        double totalCurrentYearWithSpecial = totalExpenseValue(currentYearExpenses, true);
        double totalCurrentYearWithoutSpecial = totalExpenseValue(currentYearExpenses, false);
        double totalLastYearWithSpecial = totalExpenseValue(lastYearExpenses, true);
        double totalLastYearWithoutSpecial = totalExpenseValue(lastYearExpenses, false);

        currentYearWithSpecialText.setText(numberFormatter.format(totalCurrentYearWithSpecial));
        currentYearWithoutSpecialText.setText(numberFormatter.format(totalCurrentYearWithoutSpecial));
        lastYearWithSpecialText.setText(numberFormatter.format(totalLastYearWithSpecial));
        lastYearWithoutSpecialText.setText(numberFormatter.format(totalLastYearWithoutSpecial));
        withSpecialVariationText.setText(numberFormatter.format(computeVariation(totalCurrentYearWithSpecial, totalLastYearWithSpecial)));
        withoutSpecialVariationText.setText(numberFormatter.format(computeVariation(totalCurrentYearWithoutSpecial, totalLastYearWithoutSpecial)));

        Map<ExpenseType, Double> currentYearSums = groupTotalsByExpenseType(currentYearExpenses);
        Map<ExpenseType, Double> lastYearSums = groupTotalsByExpenseType(lastYearExpenses);

        for (ExpenseType expenseType : ExpenseType.values()) {
            Double currentYearValue = currentYearSums.get(expenseType);
            Double lastYearValue = lastYearSums.get(expenseType);

            currentYearSumTexts.get(expenseType).setText(numberFormatter.format(currentYearValue));
            lastYearSumTexts.get(expenseType).setText(numberFormatter.format(lastYearValue));
            variationTexts.get(expenseType).setText(numberFormatter.format(computeVariation(currentYearValue, lastYearValue)));
        }
    }

    private double totalExpenseValue(final List<Expense> expenses, final boolean useSpecial) {
        double totalExpenses = 0;

        for (Expense expense : expenses) {
            if (!useSpecial && expense.getExpenseType() == SPECIAL) {
                continue;
            }

            totalExpenses += expense.getValue();
        }

        return totalExpenses;
    }

    private Map<ExpenseType, Double> groupTotalsByExpenseType(final List<Expense> expenses) {
        Map<ExpenseType, Double> sums = new HashMap<>();

        for (ExpenseType expenseType : ExpenseType.values()) {
            sums.put(expenseType, 0D);
        }

        for (Expense expense : expenses) {
            ExpenseType expenseType = expense.getExpenseType();

            sums.put(expenseType, sums.get(expenseType) + expense.getValue());
        }

        return sums;
    }

    private double computeVariation(final Double currentYearValue, final Double lastYearValue) {
        return lastYearValue != null ?
                (currentYearValue - lastYearValue) / lastYearValue * 100 :
                0;
    }
}
