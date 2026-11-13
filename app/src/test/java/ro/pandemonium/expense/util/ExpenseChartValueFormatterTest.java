package ro.pandemonium.expense.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieEntry;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ro.pandemonium.expense.view.ExpenseChartValueFormatter;

class ExpenseChartValueFormatterTest {

    @Test
    void barAndPieLabelsPreserveDecimalDigitsThatFloatsCannotRepresent() {
        final BigDecimal amount = new BigDecimal("999999999.99");
        final ExpenseChartValueFormatter formatter = new ExpenseChartValueFormatter();
        final String expected = ExpenseFormatUtil.amountFormatter().format(amount);

        assertEquals(expected, formatter.getBarLabel(new BarEntry(0, amount.floatValue(), amount)));
        assertEquals(expected, formatter.getPieLabel(amount.floatValue(), new PieEntry(amount.floatValue(), "Food", amount)));
    }

    @Test
    void axisLabelsUseTheSameTwoDecimalFormatAsAmounts() {
        final ExpenseChartValueFormatter formatter = new ExpenseChartValueFormatter();

        assertEquals(ExpenseFormatUtil.amountFormatter().format(new BigDecimal("12.35")), formatter.getFormattedValue(12.345F));
        assertEquals(ExpenseFormatUtil.amountFormatter().format(new BigDecimal("0.00")), formatter.getFormattedValue(0F));
    }
}
