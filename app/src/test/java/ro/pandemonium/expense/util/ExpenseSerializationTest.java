package ro.pandemonium.expense.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_DB;
import static ro.pandemonium.expense.util.ExpenseSerialization.deserialize;
import static ro.pandemonium.expense.util.ExpenseSerialization.serialize;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        final DateTimeFormatter exportDateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_DB);
        final Expense expense = new Expense();
        expense.setId(12345678L);
        expense.setExpenseType(ExpenseType.FOOD);
        expense.setDate(LocalDate.of(2026, 11, 10));
        expense.setValue(Double.valueOf(value));
        expense.setComment("Lunch");

        final String serializedExpense = serialize(expense, exportDateFormatter);

        assertEquals("12345678,FOOD,2026-11-10," + value + ",Lunch", serializedExpense);

        Locale.setDefault(importLocale);
        final DateTimeFormatter importDateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_DB);
        final Expense restoredExpense = deserialize(serializedExpense, importDateFormatter);

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
}
