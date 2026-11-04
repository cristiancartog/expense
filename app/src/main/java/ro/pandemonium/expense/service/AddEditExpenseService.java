package ro.pandemonium.expense.service;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.Constants.ADD_ACTIVITY_FORMAT_PATTERN;
import static ro.pandemonium.expense.util.StringUtils.isEmpty;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.scopes.ActivityScoped;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

@ActivityScoped
public final class AddEditExpenseService {

    private final Clock clock;
    private final DecimalFormat valueFormatter;

    private Expense expense;

    @Inject
    public AddEditExpenseService() {
        this(Clock.systemDefaultZone(), Locale.getDefault());
    }

    AddEditExpenseService(final Clock clock, final Locale locale) {
        this.clock = requireNonNull(clock);
        valueFormatter = new DecimalFormat(ADD_ACTIVITY_FORMAT_PATTERN, DecimalFormatSymbols.getInstance(requireNonNull(locale)));
        valueFormatter.setParseBigDecimal(true);
    }

    public Expense initialize(final Expense expenseToEdit) {
        expense = expenseToEdit != null ? copy(expenseToEdit) : newExpense();
        return expense;
    }

    public LocalDate selectDate(final int year, final int zeroBasedMonth, final int dayOfMonth) {
        requireInitialized();

        final LocalDate selectedDate = LocalDate.of(year, zeroBasedMonth + 1, dayOfMonth);
        expense.setDate(selectedDate);
        return selectedDate;
    }

    public boolean canSave(final String valueText) {
        return parseValue(valueText) != null;
    }

    public Expense createExpense(final ExpenseType expenseType, final String valueText, final String comment) {
        requireInitialized();
        requireNonNull(expenseType);
        requireNonNull(comment);

        final Double value = parseValue(valueText);
        if (value == null) {
            throw new IllegalArgumentException("Expense value is not valid");
        }

        expense.setExpenseType(expenseType);
        expense.setValue(value);
        expense.setComment(comment);
        return expense;
    }

    private Expense newExpense() {
        final Expense newExpense = new Expense();
        newExpense.setDate(LocalDate.now(clock));
        newExpense.setComment("");
        return newExpense;
    }

    private Expense copy(final Expense source) {
        final Expense copy = new Expense();
        copy.setId(source.getId());
        copy.setExpenseType(source.getExpenseType());
        copy.setValue(source.getValue());
        copy.setDate(requireNonNull(source.getDate()));
        copy.setComment(requireNonNull(source.getComment()));
        return copy;
    }

    private Double parseValue(final String valueText) {
        if (isEmpty(valueText)) {
            return null;
        }

        final String normalizedValue = valueText.trim();
        final ParsePosition position = new ParsePosition(0);
        final Number parsedValue = valueFormatter.parse(normalizedValue, position);
        if (parsedValue == null || position.getIndex() != normalizedValue.length()) {
            return null;
        }

        final double value = parsedValue.doubleValue();
        return Double.isFinite(value) && value >= 0D ? value : null;
    }

    private void requireInitialized() {
        if (expense == null) {
            throw new IllegalStateException("Add/edit expense service has not been initialized");
        }
    }
}
