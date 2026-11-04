package ro.pandemonium.expense.util;

import java.util.OptionalDouble;

public final class ExpenseMathUtil {

    private static final double ZERO_THRESHOLD = 0.000001D;

    private ExpenseMathUtil() {
    }

    public static OptionalDouble variation(final double currentValue, final double previousValue) {
        return Math.abs(previousValue) <= ZERO_THRESHOLD
                ? OptionalDouble.empty()
                : OptionalDouble.of((currentValue - previousValue) / previousValue);
    }
}
