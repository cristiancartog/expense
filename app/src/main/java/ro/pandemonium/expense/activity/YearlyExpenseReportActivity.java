package ro.pandemonium.expense.activity;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_TYPE;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;
import static ro.pandemonium.expense.Constants.PERCENT_FORMAT_PATTERN;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Supplier;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.YearComparisonChartActivity;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.service.YearlyReportService;
import ro.pandemonium.expense.service.YearlyReportState;
import ro.pandemonium.expense.service.YearlyReportState.Comparison;
import ro.pandemonium.expense.service.YearlyReportState.PeriodMode;
import ro.pandemonium.expense.util.DateUtil;
import ro.pandemonium.expense.util.SystemBarInsets;
import ro.pandemonium.expense.util.TaskRunner;
import ro.pandemonium.expense.view.ExpenseTypeUi;

@AndroidEntryPoint
public class YearlyExpenseReportActivity extends AppCompatActivity {

    private final NumberFormat numberFormatter = new DecimalFormat(NUMBER_FORMAT_PATTERN);
    private final NumberFormat percentFormatter = new DecimalFormat(PERCENT_FORMAT_PATTERN);

    @Inject
    YearlyReportService reportService;
    private TaskRunner reportLoader;
    private YearlyReportState reportState;
    private boolean loading;

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

    private final Map<ExpenseType, TextView> currentYearSumTexts = new HashMap<>();
    private final Map<ExpenseType, TextView> lastYearSumTexts = new HashMap<>();
    private final Map<ExpenseType, TextView> variationTexts = new HashMap<>();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.yearly_report_appbar);
        SystemBarInsets.keepContentInsideSystemBars(this);

        bindViews();
        createExpenseTypeRows();

        reportLoader = new TaskRunner(this);

        final int requestedYear = getIntent().getIntExtra(INTENT_YEAR, DateUtil.currentYear());
        loadReport(() -> reportService.initialize(requestedYear));
    }

    private void bindViews() {
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
        currentYearText.setOnClickListener(this::toggleComparisonPeriod);
    }

    private void createExpenseTypeRows() {
        final TableLayout tableLayout = findViewById(R.id.yearlyReportByExpenseTypeLayout);
        int index = 2;
        for (ExpenseType expenseType : ExpenseType.values()) {
            final TableRow tableRow = new TableRow(this);
            tableRow.setPadding(0, 5, 0, 5);
            tableRow.setOnClickListener(view -> openYearComparison(expenseType));

            final TextView expenseTypeLabel = valueTextView();
            expenseTypeLabel.setGravity(Gravity.START);
            expenseTypeLabel.setText(ExpenseTypeUi.getTextResource(expenseType));
            tableRow.addView(expenseTypeLabel);

            final TextView lastYear = valueTextView();
            tableRow.addView(lastYear);
            lastYearSumTexts.put(expenseType, lastYear);

            final TextView currentYear = valueTextView();
            tableRow.addView(currentYear);
            currentYearSumTexts.put(expenseType, currentYear);

            final TextView variation = valueTextView();
            tableRow.addView(variation);
            variationTexts.put(expenseType, variation);

            tableLayout.addView(tableRow, index++);
        }
    }

    private TextView valueTextView() {
        final TextView textView = new TextView(this);
        textView.setGravity(Gravity.END);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return textView;
    }

    private void openYearComparison(final ExpenseType expenseType) {
        if (reportState == null) {
            return;
        }
        startActivity(new Intent(this, YearComparisonChartActivity.class)
                .putExtra(INTENT_EXPENSE_TYPE, expenseType)
                .putExtra(INTENT_YEAR, reportState.year()));
    }

    private void showNextYearYearlyReport(final View ignored) {
        if (!loading) {
            loadReport(reportService::nextYear);
        }
    }

    private void showPreviousYearYearlyReport(final View ignored) {
        if (!loading) {
            loadReport(reportService::previousYear);
        }
    }

    private void toggleComparisonPeriod(final View ignored) {
        if (!loading) {
            loadReport(reportService::toggleComparisonPeriod);
        }
    }

    private void loadReport(final Supplier<YearlyReportState> operation) {
        setLoading(true);
        reportLoader.submit(operation, this::renderReport, this::showLoadError);
    }

    private void renderReport(final YearlyReportState state) {
        reportState = state;
        setLoading(false);

        currentYearText.setText(yearLabel(state));
        currentYearLabel.setText(getString(R.string.yearlyReportYearPlaceholder, state.year()));
        lastYearLabel.setText(getString(R.string.yearlyReportYearPlaceholder, state.year() - 1));

        renderComparison(state.withSpecial(), currentYearWithSpecialText,
                lastYearWithSpecialText, withSpecialVariationText);
        renderComparison(state.withoutSpecial(), currentYearWithoutSpecialText,
                lastYearWithoutSpecialText, withoutSpecialVariationText);

        for (ExpenseType expenseType : ExpenseType.values()) {
            renderComparison(
                    requireNonNull(state.byExpenseType().get(expenseType)),
                    requireNonNull(currentYearSumTexts.get(expenseType)),
                    requireNonNull(lastYearSumTexts.get(expenseType)),
                    requireNonNull(variationTexts.get(expenseType)));
        }
    }

    private String yearLabel(final YearlyReportState state) {
        if (state.periodMode() == PeriodMode.YEAR_FROM_DATE) {
            return "YFD " + state.year();
        }
        if (state.periodMode() == PeriodMode.YEAR_TO_DATE) {
            return state.year() + " YTD";
        }
        return Integer.toString(state.year());
    }

    private void renderComparison(final Comparison comparison,
                                  final TextView currentValue,
                                  final TextView previousValue,
                                  final TextView variation) {
        currentValue.setText(numberFormatter.format(comparison.currentValue()));
        previousValue.setText(numberFormatter.format(comparison.previousValue()));
        updateVariationText(variation, comparison.variation());
    }

    private void updateVariationText(final TextView textView, final OptionalDouble variation) {
        if (variation.isEmpty()) {
            textView.setText("-");
            textView.setTextColor(getColor(R.color.report_variation_negative));
            return;
        }

        final double value = variation.getAsDouble();
        textView.setTextColor(getColor(value < 0
                ? R.color.report_variation_negative
                : R.color.report_variation_positive));
        textView.setText(percentFormatter.format(value));
    }

    private void setLoading(final boolean isLoading) {
        loading = isLoading;
        currentYearText.setEnabled(!isLoading);
        previousYearButton.setEnabled(!isLoading && reportState != null && reportState.previousYearEnabled());
        nextYearButton.setEnabled(!isLoading && reportState != null && reportState.nextYearEnabled());
    }

    private void showLoadError(final Exception error) {
        setLoading(false);
        final String detail = error.getMessage() != null ? error.getMessage() : error.toString();
        Toast.makeText(this, getString(R.string.yearlyReportLoadError, detail), Toast.LENGTH_LONG)
             .show();
    }
}
