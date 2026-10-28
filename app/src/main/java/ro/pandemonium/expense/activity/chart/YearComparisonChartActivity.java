package ro.pandemonium.expense.activity.chart;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.Constants.APPLICATION_NAME;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_TYPE;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;
import static ro.pandemonium.expense.Constants.LEADING_ZERO_FORMAT;
import static ro.pandemonium.expense.Constants.PERCENT_FORMAT_PATTERN;
import static ro.pandemonium.expense.util.DateUtil.currentYear;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.YearlyReportResult;
import ro.pandemonium.expense.util.SystemBarInsets;
import ro.pandemonium.expense.util.TaskRunner;
import ro.pandemonium.expense.view.ExpenseTypeUi;

@AndroidEntryPoint
public class YearComparisonChartActivity extends AppCompatActivity {

    private static final NumberFormat VALUE_FORMATTER = new DecimalFormat(Constants.NUMBER_FORMAT_PATTERN);
    private static final NumberFormat PERCENT_FORMATTER = new DecimalFormat(PERCENT_FORMAT_PATTERN);
    private static final NumberFormat NUMBER_FORMAT = new DecimalFormat(LEADING_ZERO_FORMAT);

    private static final String[] MONTHS = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
    private static final Double ZERO_THRESHOLD = 0.000001D;
    private static final SparseArray<String> MONTHS_MAP = new SparseArray<>();

    static {
        MONTHS_MAP.put(0, "Jan");
        MONTHS_MAP.put(1, "Feb");
        MONTHS_MAP.put(2, "Mar");
        MONTHS_MAP.put(3, "Apr");
        MONTHS_MAP.put(4, "May");
        MONTHS_MAP.put(5, "Jun");
        MONTHS_MAP.put(6, "Jul");
        MONTHS_MAP.put(7, "Aug");
        MONTHS_MAP.put(8, "Sep");
        MONTHS_MAP.put(9, "Oct");
        MONTHS_MAP.put(10, "Nov");
        MONTHS_MAP.put(11, "Dec");
        MONTHS_MAP.put(-1, "???");
    }

    private int year;
    private int earliestYear;
    private int latestYear;
    private ExpenseType expenseType;

    private final Map<String, TextView> monthToLastYearValue = new HashMap<>();
    private final Map<String, TextView> monthToCurrentYearValue = new HashMap<>();
    private final Map<String, TextView> monthToVariationLabel = new HashMap<>();

    private BarChart barChart;
    private TableLayout report;
    private TextView currentYearLabel;
    private TextView lastYearReportLabel;
    private TextView currentYearReportLabel;
    private TextView lastYearTotalLabel;
    private TextView currentYearTotalLabel;

    @Inject
    ExpenseDao expenseDao;
    private TaskRunner reportLoader;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reportLoader = new TaskRunner(this);

        setContentView(R.layout.year_comparison_chart_appbar);
        SystemBarInsets.keepContentInsideSystemBars(this);

        expenseType = getIntent().getSerializableExtra(INTENT_EXPENSE_TYPE, ExpenseType.class);
        year = getIntent().getIntExtra(INTENT_YEAR, currentYear());

        barChart = findViewById(R.id.yearComparisonBarChart);
        report = findViewById(R.id.yearlyComparisonListReport);
        SwitchCompat reportSwitch = findViewById(R.id.yearComparisonSwitchChartReportButton);
        currentYearLabel = findViewById(R.id.yearlyComparisonCurrentYearText);
        TextView expenseTypeLabel = findViewById(R.id.yearComparisonChartExpenseType);
        lastYearReportLabel = findViewById(R.id.yearComparisonLastYearLabel);
        currentYearReportLabel = findViewById(R.id.yearComparisonCurrentYearLabel);
        lastYearTotalLabel = findViewById(R.id.yearComparisonLastYearTotal);
        currentYearTotalLabel = findViewById(R.id.yearComparisonCurrentYearTotal);

        reportSwitch.setOnCheckedChangeListener(this::switchChart);
        expenseTypeLabel.setText(ExpenseTypeUi.getTextResource(expenseType));

        earliestYear = expenseDao.getEarliestEntry()
                .map(expense -> expense.getDate().getYear())
                .orElse(currentYear());
        latestYear = expenseDao.getLatestEntry()
                .map(expense -> expense.getDate().getYear())
                .orElse(currentYear());

        Legend legend = barChart.getLegend();
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setTextColor(Color.WHITE);
        legend.setYOffset(0f);
        legend.setYEntrySpace(0f);
        legend.setTextSize(8f);

        XAxis xl = barChart.getXAxis();
        xl.setGranularity(1f);
        xl.setCenterAxisLabels(true);
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(final float value, final AxisBase axis) {
                return MONTHS_MAP.get((int) value);
            }
        });

        YAxis leftYAxis = barChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setSpaceTop(10f);
        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)

        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawGridBackground(false);
        barChart.setDescription(null);
        barChart.getXAxis().setAxisMinimum(0);
        barChart.getAxisRight().setEnabled(false);

        // report data
        int index = 2;
        for (String month : MONTHS) {
            TableRow tableRow = new TableRow(this);
            tableRow.setPadding(0, 5, 0, 5);

            TextView monthLabel = valueTextView();
            monthLabel.setGravity(Gravity.START);
            monthLabel.setText(MONTHS_MAP.get(parseLeadingZeroNumber(month) - 1));
            tableRow.addView(monthLabel);

            TextView lastYearLabel = valueTextView();
            monthToLastYearValue.put(month, lastYearLabel);
            tableRow.addView(lastYearLabel);

            TextView currentYearLabel = valueTextView();
            monthToCurrentYearValue.put(month, currentYearLabel);
            tableRow.addView(currentYearLabel);

            TextView variationLabel = valueTextView();
            monthToVariationLabel.put(month, variationLabel);
            tableRow.addView(variationLabel);

            report.addView(tableRow, index++);
        }

        updateDataAsync();
    }

    private int parseLeadingZeroNumber(final String leadingZeroNumber) {
        try {
            Number number = NUMBER_FORMAT.parse(leadingZeroNumber);
            requireNonNull(number);
            return number.intValue();
        } catch (ParseException e) {
            Log.w(APPLICATION_NAME, "Could not parse " + leadingZeroNumber + " ot number. Using default.");
            e.printStackTrace();
        }
        return 0;
    }

    private TextView valueTextView() {
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.END);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return textView;
    }

    private void updateDataAsync() {
        final int requestedYear = year;
        final ExpenseType requestedExpenseType = expenseType;

        reportLoader.submit(
                () -> createYearlyReport(requestedExpenseType, requestedYear),
                this::updateData);
    }

    private YearlyReportResult createYearlyReport(final ExpenseType reportExpenseType, final int reportYear) {
        final Map<String, Double> currentYearExpenses = expenseDao.getMonthlySummaryInYear(reportExpenseType, reportYear);
        final Map<String, Double> lastYearExpenses = expenseDao.getMonthlySummaryInYear(reportExpenseType, reportYear - 1);

        fillMissingMonths(currentYearExpenses);
        fillMissingMonths(lastYearExpenses);

        return new YearlyReportResult(currentYearExpenses, lastYearExpenses);
    }

    private void fillMissingMonths(final Map<String, Double> monthlyExpenses) {
        for (String month : MONTHS) {
            monthlyExpenses.putIfAbsent(month, 0D);
        }
    }

    private void updateData(final YearlyReportResult yearlyReportResult) {
        Map<String, Double> currentYearData = yearlyReportResult.getCurrentYearData();
        Map<String, Double> lastYearData = yearlyReportResult.getLastYearData();

        currentYearLabel.setText(getResources().getString(R.string.yearComparisonYearPlaceholder, year));
        currentYearReportLabel.setText(getResources().getString(R.string.yearComparisonYearPlaceholder, year));
        lastYearReportLabel.setText(getResources().getString(R.string.yearComparisonYearPlaceholder, year - 1));
        currentYearTotalLabel.setText(NUMBER_FORMAT.format(yearlyReportResult.getTotalCurrentYear()));
        lastYearTotalLabel.setText(NUMBER_FORMAT.format(yearlyReportResult.getTotalLastYear()));

        recreateDataSets(currentYearData, lastYearData);

        for (String month : MONTHS) {
            Double currentYearMonthValue = currentYearData.getOrDefault(month, 0D);
            Double lastYearMonthValue = lastYearData.getOrDefault(month, 0D);

            setTextOnNullableTextView(monthToCurrentYearValue.get(month), VALUE_FORMATTER.format(currentYearMonthValue));
            setTextOnNullableTextView(monthToLastYearValue.get(month), VALUE_FORMATTER.format(lastYearMonthValue));

            TextView variationLabel = monthToVariationLabel.get(month);
            double variation = computeVariation(currentYearMonthValue, lastYearMonthValue);
            requireNonNull(variationLabel);
            variationLabel.setText(formatVariation(currentYearMonthValue, lastYearMonthValue, variation));
            variationLabel.setTextColor(getColor(variation > 0
                    ? R.color.report_variation_positive
                    : R.color.report_variation_negative));
        }
    }

    private String formatVariation(final Double currentYearMonthValue, final Double lastYearMonthValue, final Double variation) {
        if (isZero(currentYearMonthValue) || isZero(lastYearMonthValue)) {
            return "-";
        }

        return PERCENT_FORMATTER.format(variation);
    }

    private double computeVariation(final Double currentYearMonthValue, final Double lastYearMonthValue) {
        if (isZero(lastYearMonthValue)) {
            return 0D;
        }
        return (currentYearMonthValue - lastYearMonthValue) / lastYearMonthValue;
    }

    public boolean isZero(double value) {
        return value >= -ZERO_THRESHOLD && value <= ZERO_THRESHOLD;
    }

    private void setTextOnNullableTextView(final TextView textView, final String text) {
        requireNonNull(textView);
        textView.setText(text);
    }

    private void recreateDataSets(final Map<String, Double> currentYearData,
                                  final Map<String, Double> lastYearData) {
        List<BarEntry> currentYearValues = new ArrayList<>();
        int counter = 0;
        for (String month : MONTHS) {
            BarEntry barEntry = new BarEntry(counter++, currentYearData.get(month).floatValue());
            currentYearValues.add(barEntry);
        }

        List<BarEntry> lastYearValues = new ArrayList<>();
        counter = 0;
        for (String month : MONTHS) {
            BarEntry barEntry = new BarEntry(counter++, lastYearData.get(month).floatValue());
            lastYearValues.add(barEntry);
        }

        BarDataSet currentYearDataSet = new BarDataSet(currentYearValues, year + "");
        currentYearDataSet.setColor(getColor(R.color.comparison_chart_current_year_bar));
        currentYearDataSet.setValueTextColor(Color.WHITE);
        currentYearDataSet.setValueTextSize(9);

        BarDataSet lastYearDataSet = new BarDataSet(lastYearValues, (year - 1) + "");
        lastYearDataSet.setColor(getColor(R.color.comparison_chart_last_year_bar));
        lastYearDataSet.setValueTextColor(Color.WHITE);
        lastYearDataSet.setValueTextSize(9);

        List<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(lastYearDataSet);
        dataSets.add(currentYearDataSet);

        float groupSpace = 0.04F;
        float barSpace = 0.02F;
        float barWidth = (1 - groupSpace) / dataSets.size() - barSpace;
        // (barWidth + barSpace) * nrOfExpenseTypes + groupSpace = 1.00 (interval per group)

        BarData barData = new BarData(dataSets);
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.groupBars(0, groupSpace, barSpace);
        barChart.notifyDataSetChanged();
        barChart.invalidate();
    }

    private void switchChart(final CompoundButton buttonView, final boolean isChecked) {
        barChart.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        report.setVisibility(isChecked ? View.GONE : View.VISIBLE);
    }

    public void nextYearClicked(final View view) {
        if (year < latestYear) {
            year++;
            updateDataAsync();
        }
    }

    public void previousYearClicked(final View view) {
        if (year - 1 > earliestYear) {
            year--;
            updateDataAsync();
        }
    }

}
