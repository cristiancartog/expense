package ro.pandemonium.expense.activity.chart;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.service.ExpenseHistoryService;
import ro.pandemonium.expense.service.ExpenseHistoryState;
import ro.pandemonium.expense.service.ExpenseHistoryState.MonthlyValues;
import ro.pandemonium.expense.util.LoadErrorUtil;
import ro.pandemonium.expense.util.SystemBarInsets;
import ro.pandemonium.expense.util.TaskRunner;
import ro.pandemonium.expense.view.ExpenseTypeUi;

@AndroidEntryPoint
public class ExpenseHistoryChartActivity extends AppCompatActivity {

    private static final float GROUP_SPACE = 0.04F;
    private static final float BAR_SPACE = 0.02F;

    private final Map<Float, String> pointToMonthName = new HashMap<>();

    @Inject
    ExpenseHistoryService historyService;
    private TaskRunner historyLoader;
    private BarChart barChart;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.history_chart_appbar);
        SystemBarInsets.keepContentInsideSystemBars(this);

        barChart = findViewById(R.id.expenseBarChart);
        configureChart();

        historyLoader = new TaskRunner(this);
        @SuppressWarnings("unchecked")
        final List<ExpenseType> requestedExpenseTypes = requireNonNull(getIntent().getSerializableExtra(INTENT_FILTERS, ArrayList.class));
        historyLoader.submit(
                () -> historyService.load(requestedExpenseTypes),
                this::renderHistory,
                error -> LoadErrorUtil.show(this, R.string.expenseHistoryLoadError, error)
        );
    }

    private void configureChart() {
        barChart.setDescription(null);
        barChart.setPinchZoom(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawGridBackground(false);

        final Legend legend = barChart.getLegend();
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setTextColor(Color.WHITE);
        legend.setYOffset(0f);
        legend.setYEntrySpace(0f);
        legend.setTextSize(8f);

        final XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(final float value, final AxisBase axis) {
                return pointToMonthName.getOrDefault(value, "");
            }
        });

        final YAxis leftYAxis = barChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setSpaceTop(10f);
        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setAxisMinimum(0f);

        barChart.getAxisRight().setEnabled(false);
    }

    private void renderHistory(final ExpenseHistoryState state) {
        pointToMonthName.clear();

        final Map<ExpenseType, List<BarEntry>> entriesByExpenseType = new EnumMap<>(ExpenseType.class);
        for (ExpenseType expenseType : state.expenseTypes()) {
            entriesByExpenseType.put(expenseType, new ArrayList<>());
        }

        int monthIndex = 0;
        for (MonthlyValues monthlyValues : state.monthlyValues()) {
            pointToMonthName.put((float) monthIndex, monthlyValues.yearMonth().toString());
            for (ExpenseType expenseType : state.expenseTypes()) {
                requireNonNull(entriesByExpenseType.get(expenseType)).add(new BarEntry(monthIndex, (float) monthlyValues.valueFor(expenseType)));
            }
            monthIndex++;
        }

        final Resources resources = getResources();
        final List<IBarDataSet> dataSets = new ArrayList<>();
        for (ExpenseType expenseType : state.expenseTypes()) {
            final String label = resources.getString(ExpenseTypeUi.getTextResource(expenseType));
            final BarDataSet dataSet = new BarDataSet(entriesByExpenseType.get(expenseType), label);
            dataSet.setColor(ExpenseTypeUi.getColor(expenseType));
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueTextSize(9);
            dataSets.add(dataSet);
        }

        final BarData data = new BarData(dataSets);
        final float barWidth = (1 - GROUP_SPACE) / state.expenseTypes().size() - BAR_SPACE;
        data.setBarWidth(barWidth);
        barChart.setData(data);
        barChart.getXAxis().setAxisMinimum(0);
        if (dataSets.size() > 1) {
            barChart.groupBars(0, GROUP_SPACE, BAR_SPACE);
        }
        barChart.invalidate();
    }

}
