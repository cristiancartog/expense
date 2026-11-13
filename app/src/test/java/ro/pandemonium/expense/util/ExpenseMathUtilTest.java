package ro.pandemonium.expense.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ro.pandemonium.expense.util.ExpenseMathUtil.amount;
import static ro.pandemonium.expense.util.ExpenseMathUtil.percentage;
import static ro.pandemonium.expense.util.ExpenseMathUtil.variationPercentage;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

public class ExpenseMathUtilTest {

    @Test
    public void variationCalculatesIncrease() {
        assertEquals(Optional.of(new BigDecimal("50.00")), variationPercentage(new BigDecimal("150.00"), new BigDecimal("100.00")));
    }

    @Test
    public void variationCalculatesDropToZero() {
        assertEquals(Optional.of(new BigDecimal("-100.00")), variationPercentage(new BigDecimal("0.00"), new BigDecimal("100.00")));
    }

    @Test
    public void variationIsUnavailableForZeroPreviousValue() {
        assertFalse(variationPercentage(new BigDecimal("100.00"), new BigDecimal("0.00")).isPresent());
    }

    @Test
    public void amountsHaveExactlyTwoDecimalPlaces() {
        assertEquals(new BigDecimal("0.00"), amount(BigDecimal.ZERO));
        assertEquals(new BigDecimal("12.50"), amount(new BigDecimal("12.5000")));
        assertEquals(new BigDecimal("1000.00"), amount(new BigDecimal("1E+3")));
        assertThrows(IllegalArgumentException.class, () -> amount(new BigDecimal("12.501")));
    }

    @Test
    public void percentagesRoundHalfUpAfterConversionToPercentagePoints() {
        assertEquals(new BigDecimal("33.33"), percentage(BigDecimal.ONE, BigDecimal.valueOf(3)));
        assertEquals(new BigDecimal("66.67"), percentage(BigDecimal.valueOf(2), BigDecimal.valueOf(3)));
        assertEquals(new BigDecimal("3.13"), percentage(BigDecimal.ONE, BigDecimal.valueOf(32)));
        assertEquals(new BigDecimal("-3.13"), percentage(BigDecimal.ONE.negate(), BigDecimal.valueOf(32)));
        assertEquals(new BigDecimal("0.00"), percentage(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test
    public void variationsRemainDecimalIncludingSmallChanges() {
        assertEquals(Optional.of(new BigDecimal("100.00")), variationPercentage(new BigDecimal("0.02"), new BigDecimal("0.01")));
        assertEquals(Optional.of(new BigDecimal("33.33")), variationPercentage(new BigDecimal("0.04"), new BigDecimal("0.03")));
        assertEquals(Optional.of(new BigDecimal("-3.13")), variationPercentage(new BigDecimal("31.00"), new BigDecimal("32.00")));
        assertEquals(Optional.of(new BigDecimal("0.00")), variationPercentage(new BigDecimal("999999999.99"), new BigDecimal("999999999.98")));
    }
}
