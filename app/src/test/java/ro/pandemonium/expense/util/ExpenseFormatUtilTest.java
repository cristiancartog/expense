package ro.pandemonium.expense.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.LOCALE)
class ExpenseFormatUtilTest {

    private Locale originalFormatLocale;

    @BeforeEach
    void useUsFormatting() {
        originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
    }

    @Test
    void amountsAlwaysIncludeTwoDecimalPlacesAndALeadingZero() {
        final var formatter = ExpenseFormatUtil.amountFormatter();

        assertEquals("0.00", formatter.format(BigDecimal.ZERO));
        assertEquals("0.10", formatter.format(new BigDecimal("0.1")));
        assertEquals("1,234.50", formatter.format(new BigDecimal("1234.5")));
        assertEquals("12.35", formatter.format(new BigDecimal("12.345")));
        assertEquals("-12.35", formatter.format(new BigDecimal("-12.345")));
    }

    @Test
    void percentagesAreFormattedWithoutMultiplyingThemAgain() {
        final var formatter = ExpenseFormatUtil.percentageFormatter();

        assertEquals("+33.33 %", formatter.format(new BigDecimal("33.33")));
        assertEquals("-3.13 %", formatter.format(new BigDecimal("-3.125")));
        assertEquals("+0.00 %", formatter.format(BigDecimal.ZERO));
        assertEquals("+100.00 %", formatter.format(new BigDecimal("100")));
    }

    @Test
    void entryFormattingUsesTheLocaleAndTwoDigitsWithoutGrouping() {
        assertEquals("1234.50", ExpenseFormatUtil.inputFormatter(Locale.US).format(new BigDecimal("1234.5")));
        assertEquals("1234,50", ExpenseFormatUtil.inputFormatter(Locale.forLanguageTag("ro-RO")).format(new BigDecimal("1234.5")));
    }
}
