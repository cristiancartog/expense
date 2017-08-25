package ro.pandemonium.expense.activity.chart;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.YearlyReportParams;
import ro.pandemonium.expense.model.YearlyReportResult;
import ro.pandemonium.expense.task.YearlyReportTask;

import static ro.pandemonium.expense.Constants.APPLICATION_NAME;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_TYPE;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;
import static ro.pandemonium.expense.Constants.LEADING_ZERO_FORMAT;
import static ro.pandemonium.expense.Constants.PERCENT_FORMAT_PATTERN;
import static ro.pandemonium.expense.util.DateUtil.year;

public class YearComparisonChartActivity extends AppCompatActivity
        implements View.OnClickListener, Switch.OnCheckedChangeListener {

    private static final NumberFormat VALUE_FORMATTER = new DecimalFormat(Constants.NUMBER_FORMAT_PATTERN);
    private static final NumberFormat PERCENT_FORMATTER = new DecimalFormat(PERCENT_FORMAT_PATTERN);
    private static final NumberFormat NUMBER_FORMAT = new DecimalFormat(LEADING_ZERO_FORMAT);

    private static final String[] MONTHS = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
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

    private Map<String, TextView> monthToLastYearValue = new HashMap<>();
    private Map<String, TextView> monthToCurrentYearValue = new HashMap<>();
    private Map<String, TextView> monthToVariationLabel = new HashMap<>();

    private BarChart barChart;
    private TableLayout report;
    private TextView currentYearLabel;
    private TextView lastYearReportLabel;
    private TextView currentYearReportLabel;
    private TextView lastYearTotalLabel;
    private TextView currentYearTotalLabel;

    private ExpenseDao expenseDao;
    private Resources resources;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.year_comparison_chart_appbar);

        expenseType = (ExpenseType) getIntent().getSerializableExtra(INTENT_EXPENSE_TYPE);
        year = getIntent().getIntExtra(INTENT_YEAR, year(new Date()));

        barChart = (BarChart) findViewById(R.id.yearComparisonBarChart);
        report = (TableLayout) findViewById(R.id.yearlyComparisonListReport);
        Switch reportSwitch = (Switch) findViewById(R.id.yearComparisonSwitchChartReportButton);
        currentYearLabel = (TextView) findViewById(R.id.yearlyComparisonCurrentYearText);
        TextView expenseTypeLabel = (TextView) findViewById(R.id.yearComparisonChartExpenseType);
        lastYearReportLabel = (TextView) findViewById(R.id.yearComparisonLastYearLabel);
        currentYearReportLabel = (TextView) findViewById(R.id.yearComparisonCurrentYearLabel);
        lastYearTotalLabel = (TextView) findViewById(R.id.yearComparisonLastYearTotal);
        currentYearTotalLabel = (TextView) findViewById(R.id.yearComparisonCurrentYearTotal);

        reportSwitch.setOnCheckedChangeListener(this);
        expenseTypeLabel.setText(expenseType.getTextResource());

        resources = getResources();
        expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();

        earliestYear = year(new Date(expenseDao.getEarliestEntry().getTime()));
        latestYear = year(new Date(expenseDao.getLatestEntry().getTime()));

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
        xl.setValueFormatter((value, axis) -> MONTHS_MAP.get((int) value));

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
        int number = 0;
        try {
            number = NUMBER_FORMAT.parse(leadingZeroNumber).intValue();
        } catch (ParseException e) {
            Log.w(APPLICATION_NAME, "Could not parse " + leadingZeroNumber + " ot number. Using default.");
            e.printStackTrace();
        }
        return number;
    }

    private TextView valueTextView() {
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.END);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return textView;
    }

    private void updateDataAsync() {
        new YearlyReportTask(expenseDao, this::updateData)
                .execute(new YearlyReportParams(expenseType, year));
    }

    private void updateData(final YearlyReportResult yearlyReportResult) {
        Map<String, Double> currentYearData = yearlyReportResult.getCurrentYearData();
        Map<String, Double> lastYearData = yearlyReportResult.getLastYearData();

        currentYearLabel.setText(year + "");
        currentYearReportLabel.setText(year + "");
        lastYearReportLabel.setText((year - 1) + "");
        currentYearTotalLabel.setText(NUMBER_FORMAT.format(yearlyReportResult.getTotalCurrentYear()));
        lastYearTotalLabel.setText(NUMBER_FORMAT.format(yearlyReportResult.getTotalLastYear()));

        recreateDataSets(currentYearData, lastYearData);

        for (String month : MONTHS) {
            Double currentYearMonthValue = currentYearData.get(month);
            Double lastYearMonthValue = lastYearData.get(month);

            monthToCurrentYearValue.get(month).setText(VALUE_FORMATTER.format(currentYearMonthValue));
            monthToLastYearValue.get(month).setText(VALUE_FORMATTER.format(lastYearMonthValue));

            TextView variationLabel = monthToVariationLabel.get(month);
            double variation = computeVariation(currentYearMonthValue, lastYearMonthValue);
            variationLabel.setText(currentYearMonthValue.intValue() == 0 || lastYearMonthValue.intValue() == 0
                    ? "-"
                    : PERCENT_FORMATTER.format(variation));
            variationLabel.setTextColor(resources.getColor(variation > 0
                    ? R.color.report_variation_positive
                    : R.color.report_variation_negative));
        }
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
        currentYearDataSet.setColor(resources.getColor(R.color.comparison_chart_current_year_bar));
        currentYearDataSet.setValueTextColor(Color.WHITE);
        currentYearDataSet.setValueTextSize(9);

        BarDataSet lastYearDataSet = new BarDataSet(lastYearValues, (year - 1) + "");
        lastYearDataSet.setColor(resources.getColor(R.color.comparison_chart_last_year_bar));
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

    private double computeVariation(final Double currentYearValue, final Double lastYearValue) {
        return lastYearValue != null ?
                (currentYearValue - lastYearValue) / lastYearValue :
                0;
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        if (buttonView.getId() == R.id.yearComparisonSwitchChartReportButton) {
            barChart.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            report.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.yearlyComparisonNextYear:
                if (year < latestYear) {
                    year++;
                    updateDataAsync();
                }
                break;
            case R.id.yearlyComparisonPreviousYear:
                if (year - 1 > earliestYear) {
                    year--;
                    updateDataAsync();
                }
                break;
        }
    }
}
