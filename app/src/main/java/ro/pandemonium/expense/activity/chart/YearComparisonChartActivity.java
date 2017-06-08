package ro.pandemonium.expense.activity.chart;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseArray;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.util.DateUtil;

import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_TYPE;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;

public class YearComparisonChartActivity extends Activity {

    private static final SparseArray<String> MONTHS_MAP = new SparseArray<>();

    {
        MONTHS_MAP.put(0, "JAN");
        MONTHS_MAP.put(1, "FEB");
        MONTHS_MAP.put(2, "MAR");
        MONTHS_MAP.put(3, "APR");
        MONTHS_MAP.put(4, "MAY");
        MONTHS_MAP.put(5, "JUN");
        MONTHS_MAP.put(6, "JUL");
        MONTHS_MAP.put(7, "AUG");
        MONTHS_MAP.put(8, "SEP");
        MONTHS_MAP.put(9, "OCT");
        MONTHS_MAP.put(10, "NOV");
        MONTHS_MAP.put(11, "DEC");
    }

    private final NumberFormat numberFormatter = new DecimalFormat("00");

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources resources = getResources();

        setContentView(R.layout.year_comparison_chart_activity);
        BarChart barChart = (BarChart) findViewById(R.id.yearComparisonBarChart);
        TextView expenseTypeLabel = (TextView) findViewById(R.id.yearComparisonChartExpenseType);

        ExpenseType expenseType = (ExpenseType) getIntent().getSerializableExtra(INTENT_EXPENSE_TYPE);
        int year = getIntent().getIntExtra(INTENT_YEAR, DateUtil.extractYear(new Date()));

        expenseTypeLabel.setText(expenseType.getTextResource());

        //##########################
        ExpenseDao expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();

        Map<String, Double> currentYearExpenses = expenseDao.getMonthlySummaryInYear(expenseType, year);
        Map<String, Double> lastYearExpenses = expenseDao.getMonthlySummaryInYear(expenseType, year - 1);
        fillMissingValues(currentYearExpenses, year);
        fillMissingValues(lastYearExpenses, year - 1);

        final List<IBarDataSet> dataSets = new ArrayList<>();

        List<BarEntry> currentYearValues = new ArrayList<>();
        int counter = 0;
        for (Map.Entry<String, Double> currentMonth : currentYearExpenses.entrySet()) {
            currentYearValues.add(new BarEntry(counter++, currentMonth.getValue().floatValue()));
        }
        List<BarEntry> lastYearValues = new ArrayList<>();
        counter = 0;
        for (Map.Entry<String, Double> currentMonth : lastYearExpenses.entrySet()) {
            lastYearValues.add(new BarEntry(counter++, currentMonth.getValue().floatValue()));
        }

        BarDataSet currentYearDataSet = new BarDataSet(currentYearValues, year + "");
        currentYearDataSet.setColor(resources.getColor(R.color.comparison_chart_current_year_bar));
        currentYearDataSet.setValueTextColor(Color.WHITE);
        currentYearDataSet.setValueTextSize(9);

        BarDataSet lastYearDataSet = new BarDataSet(lastYearValues, (year - 1) + "");
        lastYearDataSet.setColor(resources.getColor(R.color.comparison_chart_last_year_bar));
        lastYearDataSet.setValueTextColor(Color.WHITE);
        lastYearDataSet.setValueTextSize(9);

        dataSets.add(lastYearDataSet);
        dataSets.add(currentYearDataSet);
        //###########################

        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawGridBackground(false);

        Legend l = barChart.getLegend();
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setTextColor(Color.WHITE);
        l.setYOffset(0f);
        l.setYEntrySpace(0f);
        l.setTextSize(8f);

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

        barChart.getAxisRight().setEnabled(false);

        // data
        float groupSpace = 0.04F;
        float barSpace = 0.02F;
        float barWidth = (1 - groupSpace) / dataSets.size() - barSpace;
        // (barWidth + barSpace) * nrOfExpenseTypes + groupSpace = 1.00 (interval per group)

        BarData barData = new BarData(dataSets);
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.getXAxis().setAxisMinimum(0);
        barChart.groupBars(0, groupSpace, barSpace);
        barChart.invalidate();
    }

    private void fillMissingValues(final Map<String, Double> monthlyExpenses, final int year) {
        for (int i = 1; i <= 12; i++) {
            String month = numberFormatter.format(i);

            if (!monthlyExpenses.containsKey(month)) {
                monthlyExpenses.put(month, 0D);
            }
        }
    }

}
