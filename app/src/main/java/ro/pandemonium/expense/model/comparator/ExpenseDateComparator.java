package ro.pandemonium.expense.model.comparator;

import ro.pandemonium.expense.model.Expense;

import java.io.Serializable;
import java.util.Comparator;

public class ExpenseDateComparator implements Comparator<Expense>, Serializable {

    @Override
    public int compare(final Expense expense, final Expense expense2) {
        return expense.getTime().compareTo(expense2.getTime());
    }
}
