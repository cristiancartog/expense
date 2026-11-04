package ro.pandemonium.expense.activity.chart;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_TYPE;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;
import static ro.pandemonium.expense.Constants.PERCENT_FORMAT_PATTERN;
import static ro.pandemonium.expense.util.DateUtil.currentYear;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

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
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.service.Comparison;
import ro.pandemonium.expense.service.YearComparisonService;
import ro.pandemonium.expense.service.YearComparisonState;
import ro.pandemonium.expense.util.SystemBarInsets;
import ro.pandemonium.expense.util.TaskRunner;
import ro.pandemonium.expense.view.ExpenseTypeUi;

@AndroidEntryPoint
public class YearComparisonChartActivity extends AppCompatActivity {

    private static final NumberFormat VALUE_FORMATTER = new DecimalFormat(NUMBER_FORMAT_PATTERN);
    private static final NumberFormat PERCENT_FORMATTER = new DecimalFormat(PERCENT_FORMAT_PATTERN);

    private final Map<Month, TextView> monthToPreviousYearValue = new EnumMap<>(Month.class);
    private final Map<Month, TextView> monthToCurrentYearValue = new EnumMap<>(Month.class);
    private final Map<Month, TextView> monthToVariationLabel = new EnumMap<>(Month.class);

    @Inject
    YearComparisonService comparisonService;
    private TaskRunner reportLoader;
    private YearComparisonState comparisonState;
    private boolean loading;

    private BarChart barChart;
    private TableLayout report;
    private TextView currentYearLabel;
    private TextView expenseTypeLabel;
    private TextView previousYearReportLabel;
    private TextView currentYearReportLabel;
    private TextView previousYearTotalLabel;
    private TextView currentYearTotalLabel;
    private Button nextYearButton;
    private Button previousYearButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.year_comparison_chart_appbar);
        SystemBarInsets.keepContentInsideSystemBars(this);

        bindViews();
        configureChart();
        createMonthlyReportRows();

        reportLoader = new TaskRunner(this);
        final ExpenseType requestedExpenseType = requireNonNull(getIntent().getSerializableExtra(INTENT_EXPENSE_TYPE, ExpenseType.class));
        final int requestedYear = getIntent().getIntExtra(INTENT_YEAR, currentYear());
        loadComparison(() -> comparisonService.initialize(requestedExpenseType, requestedYear));
    }

    private void bindViews() {
        barChart = findViewById(R.id.yearComparisonBarChart);
        report = findViewById(R.id.yearlyComparisonListReport);
        final SwitchCompat reportSwitch = findViewById(R.id.yearComparisonSwitchChartReportButton);
        currentYearLabel = findViewById(R.id.yearlyComparisonCurrentYearText);
        expenseTypeLabel = findViewById(R.id.yearComparisonChartExpenseType);
        previousYearReportLabel = findViewById(R.id.yearComparisonLastYearLabel);
        currentYearReportLabel = findViewById(R.id.yearComparisonCurrentYearLabel);
        previousYearTotalLabel = findViewById(R.id.yearComparisonLastYearTotal);
        currentYearTotalLabel = findViewById(R.id.yearComparisonCurrentYearTotal);
        nextYearButton = findViewById(R.id.yearlyComparisonNextYear);
        previousYearButton = findViewById(R.id.yearlyComparisonPreviousYear);

        reportSwitch.setOnCheckedChangeListener(this::switchChart);
        nextYearButton.setOnClickListener(this::nextYearClicked);
        previousYearButton.setOnClickListener(this::previousYearClicked);
    }

    private void configureChart() {
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
                final int month = (int) value + 1;
                return month >= 1 && month <= Month.values().length
                        ? Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        : "";
            }
        });

        final YAxis leftYAxis = barChart.getAxisLeft();
        leftYAxis.setDrawGridLines(true);
        leftYAxis.setSpaceTop(10f);
        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setAxisMinimum(0f);

        barChart.setPinchZoom(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawGridBackground(false);
        barChart.setDescription(null);
        barChart.getXAxis().setAxisMinimum(0);
        barChart.getAxisRight().setEnabled(false);
    }

    private void createMonthlyReportRows() {
        int index = 2;
        for (Month month : Month.values()) {
            final TableRow tableRow = new TableRow(this);
            tableRow.setPadding(0, 5, 0, 5);

            final TextView monthLabel = valueTextView();
            monthLabel.setGravity(Gravity.START);
            monthLabel.setText(month.getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            tableRow.addView(monthLabel);

            final TextView previousYearValue = valueTextView();
            monthToPreviousYearValue.put(month, previousYearValue);
            tableRow.addView(previousYearValue);

            final TextView currentYearValue = valueTextView();
            monthToCurrentYearValue.put(month, currentYearValue);
            tableRow.addView(currentYearValue);

            final TextView variationLabel = valueTextView();
            monthToVariationLabel.put(month, variationLabel);
            tableRow.addView(variationLabel);

            report.addView(tableRow, index++);
        }
    }

    private TextView valueTextView() {
        final TextView textView = new TextView(this);
        textView.setGravity(Gravity.END);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return textView;
    }

    private void loadComparison(final Supplier<YearComparisonState> operation) {
        setLoading(true);
        reportLoader.submit(operation, this::renderComparison, this::showLoadError);
    }

    private void renderComparison(final YearComparisonState state) {
        comparisonState = state;
        setLoading(false);

        expenseTypeLabel.setText(ExpenseTypeUi.getTextResource(state.expenseType()));
        currentYearLabel.setText(getString(R.string.yearComparisonYearPlaceholder, state.year()));
        currentYearReportLabel.setText(getString(R.string.yearComparisonYearPlaceholder, state.year()));
        previousYearReportLabel.setText(getString(R.string.yearComparisonYearPlaceholder, state.year() - 1));
        currentYearTotalLabel.setText(VALUE_FORMATTER.format(state.currentYearTotal()));
        previousYearTotalLabel.setText(VALUE_FORMATTER.format(state.previousYearTotal()));

        recreateDataSets(state);

        for (Map.Entry<Month, Comparison> entry : state.monthlyComparisons().entrySet()) {
            final Comparison comparison = entry.getValue();
            requireNonNull(monthToCurrentYearValue.get(entry.getKey())).setText(VALUE_FORMATTER.format(comparison.currentValue()));
            requireNonNull(monthToPreviousYearValue.get(entry.getKey())).setText(VALUE_FORMATTER.format(comparison.previousValue()));
            updateVariation(
                    requireNonNull(monthToVariationLabel.get(entry.getKey())),
                    comparison.variation()
            );
        }
    }

    private void updateVariation(final TextView variationLabel, final OptionalDouble variation) {
        if (variation.isEmpty()) {
            variationLabel.setText("-");
            variationLabel.setTextColor(getColor(R.color.report_variation_negative));
            return;
        }

        final double value = variation.getAsDouble();
        variationLabel.setText(PERCENT_FORMATTER.format(value));
        variationLabel.setTextColor(getColor(value < 0
                ? R.color.report_variation_negative
                : R.color.report_variation_positive));
    }

    private void recreateDataSets(final YearComparisonState state) {
        final List<BarEntry> currentYearValues = new ArrayList<>();
        final List<BarEntry> previousYearValues = new ArrayList<>();
        int index = 0;
        for (Comparison comparison : state.monthlyComparisons().values()) {
            currentYearValues.add(new BarEntry(index, (float) comparison.currentValue()));
            previousYearValues.add(new BarEntry(index, (float) comparison.previousValue()));
            index++;
        }

        final BarDataSet currentYearDataSet = new BarDataSet(currentYearValues, Integer.toString(state.year()));
        currentYearDataSet.setColor(getColor(R.color.comparison_chart_current_year_bar));
        currentYearDataSet.setValueTextColor(Color.WHITE);
        currentYearDataSet.setValueTextSize(9);

        final BarDataSet previousYearDataSet = new BarDataSet(previousYearValues, Integer.toString(state.year() - 1));
        previousYearDataSet.setColor(getColor(R.color.comparison_chart_last_year_bar));
        previousYearDataSet.setValueTextColor(Color.WHITE);
        previousYearDataSet.setValueTextSize(9);

        final List<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(previousYearDataSet);
        dataSets.add(currentYearDataSet);

        final float groupSpace = 0.04F;
        final float barSpace = 0.02F;
        final float barWidth = (1 - groupSpace) / dataSets.size() - barSpace;
        final BarData barData = new BarData(dataSets);
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

    private void nextYearClicked(final View ignored) {
        if (!loading) {
            loadComparison(comparisonService::nextYear);
        }
    }

    private void previousYearClicked(final View ignored) {
        if (!loading) {
            loadComparison(comparisonService::previousYear);
        }
    }

    private void setLoading(final boolean isLoading) {
        loading = isLoading;
        previousYearButton.setEnabled(!isLoading && comparisonState != null && comparisonState.previousYearEnabled());
        nextYearButton.setEnabled(!isLoading && comparisonState != null && comparisonState.nextYearEnabled());
    }

    private void showLoadError(final Exception error) {
        setLoading(false);
        final String detail = error.getMessage() != null ? error.getMessage() : error.toString();
        Toast.makeText(this, getString(R.string.yearComparisonLoadError, detail), Toast.LENGTH_LONG)
             .show();
    }
}
