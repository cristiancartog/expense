package ro.pandemonium.expense.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import ro.pandemonium.expense.model.Expense;

public class AddEditExpenseServiceTest {

    private final AddEditExpenseService service = new AddEditExpenseService(
            Clock.fixed(Instant.parse("2026-11-04T10:15:30Z"), ZoneOffset.UTC),
            Locale.US
    );

    @Test
    public void initializeForAddUsesCurrentDateAndEmptyFormValues() {
        final Expense expense = service.initialize(null);

        assertNull(expense.getId());
        assertNull(expense.getExpenseType());
        assertNull(expense.getValue());
        assertEquals(LocalDate.of(2026, 11, 4), expense.getDate());
        assertEquals("", expense.getComment());
    }

    @Test
    public void initializeForEditReturnsADefensiveCopyOfTheExistingExpense() {
        final Expense originalExpense = expense(12L, "2026-10-20", 42.5D, "Lunch");

        final Expense initializedExpense = service.initialize(originalExpense);

        assertNotSame(originalExpense, initializedExpense);
        assertEquals(12L, initializedExpense.getId());
        assertEquals(FOOD, initializedExpense.getExpenseType());
        assertEquals(42.5D, initializedExpense.getValue());
        assertEquals(LocalDate.of(2026, 10, 20), initializedExpense.getDate());
        assertEquals("Lunch", initializedExpense.getComment());
    }

    @Test
    public void createExpensePreservesIdentityAndSelectedDateWhileApplyingFormValues() {
        service.initialize(expense(12L, "2026-10-20", 42.5D, "Lunch"));
        service.selectDate(2026, 11, 5);

        final Expense result = service.createExpense(PETROL, "18.75", "Train");

        assertEquals(12L, result.getId());
        assertEquals(PETROL, result.getExpenseType());
        assertEquals(18.75D, result.getValue());
        assertEquals(LocalDate.of(2026, 12, 5), result.getDate());
        assertEquals("Train", result.getComment());
    }

    @Test
    public void editingTheDraftDoesNotMutateTheOriginalExpense() {
        final Expense originalExpense = expense(12L, "2026-10-20", 42.5D, "Lunch");
        service.initialize(originalExpense);
        service.selectDate(2026, 11, 5);

        service.createExpense(PETROL, "18.75", "Train");

        assertEquals(FOOD, originalExpense.getExpenseType());
        assertEquals(42.5D, originalExpense.getValue());
        assertEquals(LocalDate.of(2026, 10, 20), originalExpense.getDate());
        assertEquals("Lunch", originalExpense.getComment());
    }

    @Test
    public void valueValidationRejectsBlankMalformedAndNegativeValues() {
        assertFalse(service.canSave(null));
        assertFalse(service.canSave(""));
        assertFalse(service.canSave("12 dollars"));
        assertFalse(service.canSave("-1"));
        assertTrue(service.canSave("0"));
        assertTrue(service.canSave("12.50"));
    }

    @Test
    public void valueParsingUsesTheConfiguredLocale() {
        final AddEditExpenseService romanianService = new AddEditExpenseService(
                Clock.systemUTC(),
                Locale.forLanguageTag("ro-RO")
        );
        romanianService.initialize(null);

        assertTrue(romanianService.canSave("12,50"));
        assertEquals(12.5D, romanianService.createExpense(FOOD, "12,50", "").getValue());
        assertFalse(romanianService.canSave("12.50"));
    }

    @Test
    public void stateChangingOperationsRequireInitialization() {
        assertThrows(IllegalStateException.class, () -> service.selectDate(2026, 0, 1));
        assertThrows(IllegalStateException.class, () -> service.createExpense(FOOD, "1", ""));
    }

    @Test
    public void createExpenseRejectsInvalidValues() {
        service.initialize(null);

        assertThrows(IllegalArgumentException.class, () -> service.createExpense(FOOD, "invalid", ""));
    }

    private Expense expense(final long id, final String date, final double value, final String comment) {
        final Expense expense = new Expense();
        expense.setId(id);
        expense.setExpenseType(FOOD);
        expense.setValue(value);
        expense.setDate(LocalDate.parse(date));
        expense.setComment(comment);
        return expense;
    }
}
