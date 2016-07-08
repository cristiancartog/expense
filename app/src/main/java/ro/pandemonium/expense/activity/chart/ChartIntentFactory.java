package ro.pandemonium.expense.activity.chart;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class ChartIntentFactory {

    private static final Random random = new Random();

    private static int randomColor () {
        final int minDark = 56;
        final int remaining = 256 - minDark;
        return Color.rgb(minDark + random.nextInt(remaining), minDark + random.nextInt(remaining), minDark + random.nextInt(remaining));
    }

    private static Map<ExpenseType, List<Expense>> groupByExpenseType(final List<Expense> expenses) {
        final Map<ExpenseType, List<Expense>> mapExpenseToExpenseType = new HashMap<>();
        for(Expense expense: expenses) {
            final ExpenseType expenseType = expense.getExpenseType();
            List<Expense> expenseSubList = mapExpenseToExpenseType.get(expenseType);
            if (expenseSubList == null) {
                expenseSubList = new ArrayList<>();
                mapExpenseToExpenseType.put(expenseType, expenseSubList);
            }
            expenseSubList.add(expense);
        }
        return mapExpenseToExpenseType;
    }

    public static Intent createLineChartIntent(final Context context, final List<Expense> expenses) {
        final Map<ExpenseType, List<Expense>> mapExpenseTypeToExpenses = groupByExpenseType(expenses);

        final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        final XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[] {0, 0, 0, 0});
        renderer.setAxesColor(Color.DKGRAY);
        renderer.setLabelsColor(Color.LTGRAY);

        for (Map.Entry<ExpenseType, List<Expense>> entry: mapExpenseTypeToExpenses.entrySet()) {
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(randomColor());
            r.setPointStyle(PointStyle.SQUARE);
            r.setFillPoints(true);
            renderer.addSeriesRenderer(r);

            final List<Expense> expenseSubList = entry.getValue();
            TimeSeries series = new TimeSeries(entry.getKey().name());
            for (Expense expense : expenseSubList) {
                series.add(expense.getDate(), expense.getValue());
            }
            dataset.addSeries(series);
        }

        return ChartFactory.getTimeChartIntent(context, dataset, renderer, null);
    }
}
