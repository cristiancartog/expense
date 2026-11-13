package ro.pandemonium.expense.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ro.pandemonium.expense.util.ExpenseSerialization.deserialize;
import static ro.pandemonium.expense.util.ExpenseSerialization.serialize;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.util.Locale;
import java.util.stream.Stream;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

@ResourceLock(Resources.LOCALE)
class ExpenseSerializationTest {

    private Locale originalLocale;
    private Locale originalDisplayLocale;
    private Locale originalFormatLocale;

    @BeforeEach
    void saveLocale() {
        originalLocale = Locale.getDefault();
        originalDisplayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        originalFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
        Locale.setDefault(Locale.Category.DISPLAY, originalDisplayLocale);
        Locale.setDefault(Locale.Category.FORMAT, originalFormatLocale);
    }

    @ParameterizedTest(name = "{0} to {1}: {2}")
    @MethodSource("localeTransfers")
    void preservesFractionalValueAndCommentAcrossLocales(final Locale exportLocale, final Locale importLocale, final String value) {
        Locale.setDefault(exportLocale);
        final Expense expense = new Expense();
        expense.setId(12345678L);
        expense.setExpenseType(ExpenseType.FOOD);
        expense.setDate(LocalDate.of(2026, 11, 10));
        expense.setValue(new BigDecimal(value));
        expense.setComment("Lunch");

        final String serializedExpense = serialize(expense);

        assertEquals("12345678,FOOD,2026-11-10," + value + ",Lunch", serializedExpense);

        Locale.setDefault(importLocale);
        final Expense restoredExpense = deserialize(serializedExpense);

        assertEquals(expense.getId(), restoredExpense.getId());
        assertEquals(expense.getExpenseType(), restoredExpense.getExpenseType());
        assertEquals(expense.getDate(), restoredExpense.getDate());
        assertEquals(expense.getValue(), restoredExpense.getValue());
        assertEquals(expense.getComment(), restoredExpense.getComment());
    }

    private static Stream<Arguments> localeTransfers() {
        return Stream.of(Locale.US, Locale.forLanguageTag("ro-RO"), Locale.forLanguageTag("ar-EG"))
                     .flatMap(exportLocale -> Stream.of(Locale.US, Locale.forLanguageTag("ro-RO"), Locale.forLanguageTag("ar-EG"))
                                                    .flatMap(importLocale -> Stream.of("12.50", "0.01", "1234.56")
                                                                                          .map(value -> Arguments.of(exportLocale, importLocale, value))
                                                    )
                     );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", "1,FOOD,2026-11-10", ",FOOD,2026-11-10,12.50", "x,FOOD,2026-11-10,12.50",
            "1,,2026-11-10,12.50", "1,UNKNOWN,2026-11-10,12.50", "1,FOOD,2026-11-10,"
    })
    void rejectsInvalidRecords(final String record) {
        assertThrows(IllegalArgumentException.class, () -> deserialize(record));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-02-30", "2026-02-29", "2026-04-31", "2026-13-01", "2026-00-10", "2026-01-00", ""})
    void rejectsInvalidDates(final String date) {
        assertThrows(
                DateTimeException.class,
                () -> deserialize("1,FOOD," + date + ",12.50")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.01", "12.50", "12.5000", "999999999.99"})
    void acceptsSupportedAmountsAndLeapDays(final String amount) {
        final Expense expense = deserialize("1,FOOD,2024-02-29," + amount + ",");

        assertEquals(new BigDecimal(amount).setScale(2), expense.getValue());
        assertEquals(LocalDate.of(2024, 2, 29), expense.getDate());
        assertEquals("", expense.getComment());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ",", ",Lunch, coffee"})
    void preservesOptionalComments(final String suffix) {
        final Expense expense = deserialize("1,FOOD,2026-11-10,12.50" + suffix);

        assertEquals(suffix.isEmpty() ? "" : suffix.substring(1), expense.getComment());
    }
}
