package ro.pandemonium.expense.activity;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_TYPE;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;
import static ro.pandemonium.expense.Constants.PERCENT_FORMAT_PATTERN;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;
import static ro.pandemonium.expense.util.DateUtil.addYears;
import static ro.pandemonium.expense.util.DateUtil.endOfYear;
import static ro.pandemonium.expense.util.DateUtil.startOfYear;
import static ro.pandemonium.expense.util.DateUtil.year;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.YearComparisonChartActivity;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.util.DateUtil;
import ro.pandemonium.expense.util.SystemBarInsets;
import ro.pandemonium.expense.view.ExpenseTypeUi;

public class YearlyExpenseReportActivity extends AppCompatActivity {

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
    private LocalDate earliestEntryDate;
    private LocalDate latestEntryDate;
    private boolean useYearToDate;
    private boolean useYearFromDate;

    private final Map<ExpenseType, TextView> currentYearSumTexts = new HashMap<>();
    private final Map<ExpenseType, TextView> lastYearSumTexts = new HashMap<>();
    private final Map<ExpenseType, TextView> variationTexts = new HashMap<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.yearly_report_appbar);
        SystemBarInsets.keepContentInsideSystemBars(this);

        currentYearText = findViewById(R.id.yearlyReportCurrentYearText);
        currentYearLabel = findViewById(R.id.yearlyReportCurrentYearLabel);
        lastYearLabel = findViewById(R.id.yearlyReportLastYearLabel);
        currentYearWithSpecialText = findViewById(R.id.yearlyReportCurrentYearWithSpecial);
        lastYearWithSpecialText = findViewById(R.id.yearlyReportLastYearWithSpecial);
        currentYearWithoutSpecialText = findViewById(R.id.yearlyReportCurrentYearWithoutSpecial);
        lastYearWithoutSpecialText = findViewById(R.id.yearlyReportLastYearWithoutSpecial);
        withSpecialVariationText = findViewById(R.id.yearlyReportWithSpecialVariation);
        withoutSpecialVariationText = findViewById(R.id.yearlyReportWithoutSpecialVariation);
        nextYearButton = findViewById(R.id.yearlyReportNextYear);
        previousYearButton = findViewById(R.id.yearlyReportPreviousYear);

        nextYearButton.setOnClickListener(this::showNextYearYearlyReport);
        previousYearButton.setOnClickListener(this::showPreviousYearYearlyReport);
        currentYearText.setOnClickListener(this::showCurrentYearYearlyReport);

        expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();

        year = getIntent().getIntExtra(INTENT_YEAR, DateUtil.currentYear());

        TableLayout tableLayout = findViewById(R.id.yearlyReportByExpenseTypeLayout);

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
            expenseTypeLabel.setText(ExpenseTypeUi.getTextResource(expenseType));
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

        Expense earliestExpense = expenseDao.getEarliestEntry();
        Expense latestExpense = expenseDao.getLatestEntry();
        earliestEntryDate = earliestExpense != null ? earliestExpense.getDate() : LocalDate.now();
        latestEntryDate = latestExpense != null ? latestExpense.getDate() : LocalDate.now();
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

    private void showNextYearYearlyReport(final View v) {
        year++;
        useYearToDate = false;
        useYearFromDate = false;
        updateYearButtonsEnabledState();
        recomputeReport();
    }

    private void showPreviousYearYearlyReport(final View v) {
        year--;
        useYearToDate = false;
        useYearFromDate = false;
        updateYearButtonsEnabledState();
        recomputeReport();
    }

    private void showCurrentYearYearlyReport(final View v) {
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
    }

    private void updateYearButtonsEnabledState() {
        previousYearButton.setEnabled((year - 1) > earliestEntryYear);
        nextYearButton.setEnabled(year < latestEntryYear);
    }

    private void recomputeReport() {
        String yearText = year + "";
        if (useYearFromDate) {
            yearText = "YFD " + yearText;
        } else if (useYearToDate) {
            yearText = year + " YTD";
        }
        currentYearText.setText(yearText);

        currentYearLabel.setText(getResources().getString(R.string.yearlyReposrYearPlaceholder, year));
        lastYearLabel.setText(getResources().getString(R.string.yearlyReposrYearPlaceholder, year - 1));

        LocalDate currentYearExpensesStartDate = useYearFromDate ? addYears(earliestEntryDate, 1) : startOfYear(year);
        LocalDate currentYearExpensesEndDate = useYearToDate ? latestEntryDate : endOfYear(year);
        LocalDate lastYearExpensesStartDate = useYearFromDate ? earliestEntryDate : startOfYear(year - 1);
        LocalDate lastYearExpensesEndDate = useYearToDate ? addYears(latestEntryDate, -1) : endOfYear(year - 1);

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

            updateSumText(currentYearSumTexts.get(expenseType), numberFormatter.format(currentYearValue));
            updateSumText(lastYearSumTexts.get(expenseType), numberFormatter.format(lastYearValue));
            updateVariationText(variationTexts.get(expenseType), computeVariation(currentYearValue, lastYearValue));
        }
    }

    private void updateSumText(final TextView textView, final String value) {
        requireNonNull(textView);
        textView.setText(value);
    }

    private void updateVariationText(final TextView textView, double value) {
        requireNonNull(textView);
        textView.setTextColor(getColor(value < 0
                ? R.color.report_variation_negative
                : R.color.report_variation_positive));
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
        Map<ExpenseType, Double> totals = expenses.stream()
                .collect(
                    groupingBy(
                        Expense::getExpenseType,
                        () -> new EnumMap<>(ExpenseType.class),
                        summingDouble(Expense::getValue)
                ));

        // Ensure all enum keys are present with a default of 0.0
        for (ExpenseType type : ExpenseType.values()) {
            totals.putIfAbsent(type, 0.0);
        }

        return totals;
    }

    private double computeVariation(final Double currentYearValue, final Double lastYearValue) {
        return lastYearValue != null ?
                (currentYearValue - lastYearValue) / lastYearValue :
                0;
    }
}
