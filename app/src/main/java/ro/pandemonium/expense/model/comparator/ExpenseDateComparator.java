package ro.pandemonium.expense.model.comparator;

import ro.pandemonium.expense.model.Expense;

import java.util.Comparator;

public class ExpenseDateComparator implements Comparator<Expense> {

    private final boolean isAscending;

    public ExpenseDateComparator(final boolean isAscending) {
        this.isAscending = isAscending;
    }

    @Override
    public int compare(final Expense expense, final Expense expense2) {
        return (isAscending ? 1 : -1) * expense.getDate().compareTo(expense2.getDate());
    }
}
