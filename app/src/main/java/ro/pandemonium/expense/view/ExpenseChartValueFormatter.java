package ro.pandemonium.expense.view;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.math.BigDecimal;
import java.text.NumberFormat;

import ro.pandemonium.expense.util.ExpenseFormatUtil;

public final class ExpenseChartValueFormatter extends ValueFormatter {

    private final NumberFormat formatter = ExpenseFormatUtil.amountFormatter();

    @Override
    public String getFormattedValue(final float value) {
        return formatter.format(new BigDecimal(Float.toString(value)));
    }

    @Override
    public String getBarLabel(final BarEntry entry) {
        return entry.getData() instanceof BigDecimal amount ? formatter.format(amount) : getFormattedValue(entry.getY());
    }

    @Override
    public String getPieLabel(final float value, final PieEntry entry) {
        return entry.getData() instanceof BigDecimal amount ? formatter.format(amount) : getFormattedValue(value);
    }
}
