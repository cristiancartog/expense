package ro.pandemonium.expense.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.YearComparisonChartActivity;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_TYPE;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;
import static ro.pandemonium.expense.Constants.PERCENT_FORMAT_PATTERN;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;
import static ro.pandemonium.expense.util.DateUtil.addYears;
import static ro.pandemonium.expense.util.DateUtil.endOfYear;
import static ro.pandemonium.expense.util.DateUtil.year;
import static ro.pandemonium.expense.util.DateUtil.startOfYear;

public class YearlyExpenseReportActivity extends Activity implements View.OnClickListener {

    private final NumberFormat numberFormatter = new DecimalFormat(NUMBER_FORMAT_PATTERN);
    private final NumberFormat percentFormatter = new DecimalFormat(PERCENT_FORMAT_PATTERN);

    private ExpenseDao expenseDao;

    private TextView currentYearText;
    private TextView currentYearLabel;
    private TextView lastYearLabel;
    private TextView currentYearWithSpecialText;
    private TextView lastYearWithSpecialText;
    private TextView currentYearWithoutSpecialText;
    private TextView lastYearWithoutSpecialText;
    private TextView withSpecialVariationText;
    private TextView withoutSpecialVariationText;
    private Button nextYearButton;
    private Button previousYearButton;

    private int year;
    private int earliestEntryYear;
    private int latestEntryYear;
    private Date earliestEntryDate;
    private Date latestEntryDate;
    private boolean useYearToDate;
    private boolean useYearFromDate;

    private Map<ExpenseType, TextView> currentYearSumTexts = new HashMap<>();
    private Map<ExpenseType, TextView> lastYearSumTexts = new HashMap<>();
    private Map<ExpenseType, TextView> variationTexts = new HashMap<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.yearly_report_activity);

        currentYearText = (TextView) findViewById(R.id.yearlyReportCurrentYearText);
        currentYearLabel = (TextView) findViewById(R.id.yearlyReportCurrentYearLabel);
        lastYearLabel = (TextView) findViewById(R.id.yearlyReportLastYearLabel);
        currentYearWithSpecialText = (TextView) findViewById(R.id.yearlyReportCurrentYearWithSpecial);
        lastYearWithSpecialText = (TextView) findViewById(R.id.yearlyReportLastYearWithSpecial);
        currentYearWithoutSpecialText = (TextView) findViewById(R.id.yearlyReportCurrentYearWithoutSpecial);
        lastYearWithoutSpecialText = (TextView) findViewById(R.id.yearlyReportLastYearWithoutSpecial);
        withSpecialVariationText = (TextView) findViewById(R.id.yearlyReportWithSpecialVariation);
        withoutSpecialVariationText = (TextView) findViewById(R.id.yearlyReportWithoutSpecialVariation);
        nextYearButton = (Button) findViewById(R.id.yearlyReportNextYear);
        previousYearButton = (Button) findViewById(R.id.yearlyReportPreviousYear);

        currentYearText.setOnClickListener(this);
        nextYearButton.setOnClickListener(this);
        previousYearButton.setOnClickListener(this);

        expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();

        year = getIntent().getExtras().getInt(INTENT_YEAR);

        TableLayout tableLayout = (TableLayout) findViewById(R.id.yearlyReportByExpenseTypeLayout);

        int index = 2;
        for (ExpenseType expenseType : ExpenseType.values()) {
            TableRow tableRow = new TableRow(this);
            tableRow.setPadding(0, 5, 0, 5);
            tableRow.setOnClickListener(view ->
                    startActivity(new Intent(this, YearComparisonChartActivity.class)
                            .putExtra(INTENT_EXPENSE_TYPE, expenseType)
                            .putExtra(INTENT_YEAR, year)));

            TextView expenseTypeLabel = valueTextView();
            expenseTypeLabel.setGravity(Gravity.START);
            expenseTypeLabel.setText(expenseType.getTextResource());
            tableRow.addView(expenseTypeLabel);

            TextView lastYear = valueTextView();
            tableRow.addView(lastYear);
            lastYearSumTexts.put(expenseType, lastYear);

            TextView currentYear = valueTextView();
            tableRow.addView(currentYear);
            currentYearSumTexts.put(expenseType, currentYear);

            TextView variation = valueTextView();
            tableRow.addView(variation);
            variationTexts.put(expenseType, variation);

            tableLayout.addView(tableRow, index++);
        }

        earliestEntryDate = new Date(expenseDao.getEarliestEntry().getTime());
        latestEntryDate = new Date(expenseDao.getLatestEntry().getTime());
        earliestEntryYear = year(earliestEntryDate);
        latestEntryYear = year(latestEntryDate);
        useYearToDate = latestEntryYear == year;
        useYearFromDate = earliestEntryYear == year;

        updateYearButtonsEnabledState();

        recomputeReport();
    }

    private TextView valueTextView() {
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.END);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return textView;
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()) {

            case R.id.yearlyReportNextYear:
                year++;
                useYearToDate = false;
                useYearFromDate = false;
                updateYearButtonsEnabledState();
                recomputeReport();
                break;

            case R.id.yearlyReportPreviousYear:
                year--;
                useYearToDate = false;
                useYearFromDate = false;
                updateYearButtonsEnabledState();
                recomputeReport();
                break;

            case R.id.yearlyReportCurrentYearText:
                if ((year - 1) == earliestEntryYear) {
                    useYearFromDate = !useYearFromDate;
                    recomputeReport();
                } else if (year == latestEntryYear) {
                    useYearToDate = !useYearToDate;
                    recomputeReport();
                } else {
                    useYearToDate = false;
                    useYearFromDate = false;
                }

                break;
        }
    }

    private void updateYearButtonsEnabledState() {
        previousYearButton.setEnabled((year - 1) != earliestEntryYear);
        nextYearButton.setEnabled(year != latestEntryYear);
    }

    private void recomputeReport() {
        String yearText = year + "";
        if (useYearFromDate) {
            yearText = "YFD " + yearText;
        } else if (useYearToDate) {
            yearText = year + " YTD";
        }
        currentYearText.setText(yearText);

        currentYearLabel.setText(year + "");
        lastYearLabel.setText((year - 1) + "");

        Date currentYearExpensesStartDate = useYearFromDate ? addYears(earliestEntryDate, 1) : startOfYear(year);
        Date currentYearExpensesEndDate = useYearToDate ? latestEntryDate : endOfYear(year);
        Date lastYearExpensesStartDate = useYearFromDate ? earliestEntryDate : startOfYear(year - 1);
        Date lastYearExpensesEndDate = useYearToDate ? addYears(latestEntryDate, -1) : endOfYear(year - 1);

        List<Expense> currentYearExpenses = expenseDao.getExpensesInInterval(currentYearExpensesStartDate, currentYearExpensesEndDate);
        List<Expense> lastYearExpenses = expenseDao.getExpensesInInterval(lastYearExpensesStartDate, lastYearExpensesEndDate);

        double totalCurrentYearWithSpecial = totalExpenseValue(currentYearExpenses, true);
        double totalCurrentYearWithoutSpecial = totalExpenseValue(currentYearExpenses, false);
        double totalLastYearWithSpecial = totalExpenseValue(lastYearExpenses, true);
        double totalLastYearWithoutSpecial = totalExpenseValue(lastYearExpenses, false);

        currentYearWithSpecialText.setText(numberFormatter.format(totalCurrentYearWithSpecial));
        currentYearWithoutSpecialText.setText(numberFormatter.format(totalCurrentYearWithoutSpecial));
        lastYearWithSpecialText.setText(numberFormatter.format(totalLastYearWithSpecial));
        lastYearWithoutSpecialText.setText(numberFormatter.format(totalLastYearWithoutSpecial));
        updateVariationText(withSpecialVariationText, computeVariation(totalCurrentYearWithSpecial, totalLastYearWithSpecial));
        updateVariationText(withoutSpecialVariationText, computeVariation(totalCurrentYearWithoutSpecial, totalLastYearWithoutSpecial));

        Map<ExpenseType, Double> currentYearSums = groupTotalsByExpenseType(currentYearExpenses);
        Map<ExpenseType, Double> lastYearSums = groupTotalsByExpenseType(lastYearExpenses);

        for (ExpenseType expenseType : ExpenseType.values()) {
            Double currentYearValue = currentYearSums.get(expenseType);
            Double lastYearValue = lastYearSums.get(expenseType);

            currentYearSumTexts.get(expenseType).setText(numberFormatter.format(currentYearValue));
            lastYearSumTexts.get(expenseType).setText(numberFormatter.format(lastYearValue));

            double variation = computeVariation(currentYearValue, lastYearValue);
            TextView variationText = variationTexts.get(expenseType);
            updateVariationText(variationText, variation);
        }
    }

    private void updateVariationText(final TextView textView, double value) {
        textView.setTextColor(getResources().getColor(value < 0 ? R.color.report_variation_negative : R.color.report_variation_positive));
        textView.setText(percentFormatter.format(value));
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
                (currentYearValue - lastYearValue) / lastYearValue :
                0;
    }
}
