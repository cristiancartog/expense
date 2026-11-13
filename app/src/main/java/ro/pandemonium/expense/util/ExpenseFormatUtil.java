package ro.pandemonium.expense.util;

import static ro.pandemonium.expense.util.ExpenseMathUtil.ROUNDING_MODE;
import static ro.pandemonium.expense.util.ExpenseMathUtil.SCALE;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class ExpenseFormatUtil {

    private static final String NUMBER_FORMAT_PATTERN = "#,##0.00";
    private static final String PERCENT_FORMAT_PATTERN = "+0.00 '%';-0.00 '%'";
    private static final String ADD_ACTIVITY_FORMAT_PATTERN = "0.00";

    private ExpenseFormatUtil() {
    }

    public static DecimalFormat amountFormatter() {
        return formatter(NUMBER_FORMAT_PATTERN, Locale.getDefault(Locale.Category.FORMAT));
    }

    public static DecimalFormat inputFormatter(final Locale locale) {
        return formatter(ADD_ACTIVITY_FORMAT_PATTERN, locale);
    }

    // Percentages are already expressed in percentage points, so the '%' suffix is literal.
    public static DecimalFormat percentageFormatter() {
        return formatter(PERCENT_FORMAT_PATTERN, Locale.getDefault(Locale.Category.FORMAT));
    }

    private static DecimalFormat formatter(final String pattern, final Locale locale) {
        final DecimalFormat formatter = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale));
        formatter.setMinimumFractionDigits(SCALE);
        formatter.setMaximumFractionDigits(SCALE);
        formatter.setRoundingMode(ROUNDING_MODE);
        formatter.setParseBigDecimal(true);
        return formatter;
    }
}
