package ro.pandemonium.expense.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.pandemonium.expense.util.ExpenseMathUtil.variation;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

public class ExpenseMathUtilTest {

    private static final double DELTA = 0.000001D;

    @Test
    public void variationCalculatesIncrease() {
        assertVariation(variation(150D, 100D), 0.5D);
    }

    @Test
    public void variationCalculatesDropToZero() {
        assertVariation(variation(0D, 100D), -1D);
    }

    @Test
    public void variationIsUnavailableForEffectivelyZeroPreviousValue() {
        assertFalse(variation(100D, 0D).isPresent());
        assertFalse(variation(100D, 0.0000001D).isPresent());
    }

    private void assertVariation(final OptionalDouble variation, final double expected) {
        assertTrue(variation.isPresent());
        assertEquals(expected, variation.getAsDouble(), DELTA);
    }
}
