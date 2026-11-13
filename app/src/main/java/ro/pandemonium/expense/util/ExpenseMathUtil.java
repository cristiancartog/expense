package ro.pandemonium.expense.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public final class ExpenseMathUtil {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ExpenseMathUtil() {
    }

    // Amount boundaries are exact: accept trailing zeros, but never silently change money.
    public static BigDecimal amount(final BigDecimal value) {
        try {
            return value.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Expense amounts must have at most two decimal places", e);
        }
    }

    // Return percentage points (33.33 means 33.33%), rounding only after multiplying by 100.
    public static BigDecimal percentage(final BigDecimal part, final BigDecimal total) {
        final BigDecimal normalizedPart = amount(part);
        final BigDecimal normalizedTotal = amount(total);
        return normalizedTotal.signum() == 0
                ? ZERO
                : normalizedPart.multiply(HUNDRED)
                                .divide(normalizedTotal, SCALE, ROUNDING_MODE);
    }

    public static Optional<BigDecimal> variationPercentage(final BigDecimal currentValue, final BigDecimal previousValue) {
        final BigDecimal current = amount(currentValue);
        final BigDecimal previous = amount(previousValue);
        return previous.signum() == 0
                ? Optional.empty()
                : Optional.of(percentage(current.subtract(previous), previous));
    }
}
