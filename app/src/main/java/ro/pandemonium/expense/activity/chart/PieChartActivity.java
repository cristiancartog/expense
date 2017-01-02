package ro.pandemonium.expense.activity.chart;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.view.ExpenseTypeColor;

public class PieChartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pie_chart_activity);

        PieChart pieChart = (PieChart) findViewById(R.id.expensePieChart);
        pieChart.setDescription(null);

        @SuppressWarnings("unchecked")
        Map<ExpenseType, Double> expenseTypeValues = (Map<ExpenseType, Double>) getIntent()
                .getSerializableExtra(Constants.INTENT_EXPENSE_VALUES_BY_TYPE);

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

        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setData(data);
        pieChart.highlightValues(null); // undo all highlights

        pieChart.invalidate();
    }
}
