package ro.pandemonium.expense.activity.chart;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.ExpenseMonthlySummary;
import ro.pandemonium.expense.model.ExpenseType;

public class BarChartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bar_chart_activity);


        final ExpenseDao expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();

        @SuppressWarnings("unchecked")
        List<ExpenseType> expenseTypes = (List<ExpenseType>) getIntent().getSerializableExtra(Constants.INTENT_FILTERS);

        List<ExpenseMonthlySummary> monthlySummary = expenseDao.getMonthlySummary2(expenseTypes);

        BarChart barChart = (BarChart) findViewById(R.id.expenseBarChart);

        barChart.setDescription("Monthly expense type summary");

        final Map<Float, String> mapPointToMonthName = new HashMap<>();

        // scaling can now only be done on x- and y-axis separately
        barChart.setPinchZoom(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawGridBackground(false);

        Legend l = barChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
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
        xl.setValueFormatter(new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mapPointToMonthName.get(value);
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });

        YAxis leftYAxis = barChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setSpaceTop(10f);
        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setAxisMinValue(0f); // this replaces setStartAtZero(true)

        barChart.getAxisRight().setEnabled(false);



        // data
        float groupSpace = 0.04f;
        float barSpace = 0.02f;
        float barWidth = (1 - groupSpace) / expenseTypes.size() - barSpace;
        // (barWidth + barSpace) * nrOfExpenseTypes + groupSpace = 1.00 (interval per group)

        final List<IBarDataSet> dataSets = new ArrayList<>();

        final Resources resources = getResources();
        final Map<ExpenseType, List<BarEntry>> yValuesMap = new HashMap<>();

        for (ExpenseType expenseType: expenseTypes) {
            yValuesMap.put(expenseType, new ArrayList<BarEntry>());
        }

        int counter = 0;
        for (ExpenseMonthlySummary summary: monthlySummary) {
            Map<ExpenseType, Double> monthlyValues = summary.getValues();
            for (ExpenseType expenseType: expenseTypes) {
                List<BarEntry> yValues = yValuesMap.get(expenseType);

                Double value = monthlyValues.get(expenseType);
                yValues.add(new BarEntry(counter, value != null ? value.floatValue() : 0));
            }

            mapPointToMonthName.put((float)counter, summary.getYearMonth());

            counter++;
        }

        for (ExpenseType expenseType: expenseTypes) {
            String text = resources.getString(expenseType.getTextResource());
            BarDataSet barDataSet = new BarDataSet(yValuesMap.get(expenseType), text);
            barDataSet.setColor(expenseType.getColor());
            barDataSet.setValueTextColor(Color.WHITE);
            barDataSet.setValueTextSize(9);

            dataSets.add(barDataSet);
        }

        BarData data = new BarData(dataSets);

        barChart.setData(data);


        barChart.getBarData().setBarWidth(barWidth);
        barChart.getXAxis().setAxisMinValue(0);
        if (dataSets.size() > 1) {
            barChart.groupBars(0, groupSpace, barSpace);
        }

        barChart.invalidate();
    }
}