package ro.pandemonium.expense.activity.chart;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.view.ExpenseTypeColor;

import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.Constants.INTENT_MONTH;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;
import static ro.pandemonium.expense.util.DateUtil.currentMonth;
import static ro.pandemonium.expense.util.DateUtil.currentYear;

public class CurrentMonthPieChartActivity extends Activity implements View.OnClickListener {

    private final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault());

    private Calendar calendar = Calendar.getInstance();
    private TextView currentMonthLabel;
    private PieChart pieChart;

    private ExpenseDao expenseDao;
    private int year;
    private int month;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pie_chart_activity);

        year = getIntent().getExtras().getInt(INTENT_YEAR, currentYear());
        month = getIntent().getExtras().getInt(INTENT_MONTH, currentMonth());

        pieChart = (PieChart) findViewById(R.id.monthPieChart);
        currentMonthLabel = (TextView) findViewById(R.id.monthPieChartCurrentMonthLabel);
        expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();

        pieChart.setDescription(null);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.highlightValues(null); // undo all highlights

        refillData();
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.monthPieChartPreviousMonth:
                month--;
                if (month < 1) {
                    month = 12;
                    year--;
                }

                refillData();
                break;
            case R.id.monthPieChartNextMonth:
                month++;
                if (month > 12) {
                    month = 1;
                    year++;
                }

                refillData();
                break;
        }
    }

    private void refillData() {
        updateMonthButtonLabel();

        Map<ExpenseType, Double> expenseTypeValues = ExpenseUtil.computeExpenseSumByCategory(expenseDao.fetchExpenses(year, month));

        final List<PieEntry> entries = new ArrayList<>();
        final Resources resources = getResources();
        for (Map.Entry<ExpenseType, Double> entry : expenseTypeValues.entrySet()) {
            String text = resources.getString(entry.getKey().getTextResource());
            entries.add(new PieEntry(entry.getValue().floatValue(), text));
        }

        final PieDataSet pieDataSet = new PieDataSet(entries, "Expense types");

        pieDataSet.setSliceSpace(5f);
        pieDataSet.setSelectionShift(15f);

        ArrayList<Integer> colors = new ArrayList<>();
        for (ExpenseType expenseType : ExpenseType.values()) {
            int color = ExpenseTypeColor.color(expenseType);
            if (color != Color.WHITE) {
                colors.add(color);
            }
        }

        pieDataSet.setColors(colors);

        PieData data = new PieData(pieDataSet);
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.notifyDataSetChanged();
        pieChart.invalidate();

    }

    private void updateMonthButtonLabel() {
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.YEAR, year);
        currentMonthLabel.setText(dateFormat.format(calendar.getTime()));
    }
}
