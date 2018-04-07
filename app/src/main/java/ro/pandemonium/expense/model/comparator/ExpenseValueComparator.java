package ro.pandemonium.expense.model.comparator;

import ro.pandemonium.expense.model.Expense;

import java.io.Serializable;
import java.util.Comparator;

public class ExpenseValueComparator implements Comparator<Expense>, Serializable {

    @Override
    public int compare(final Expense expense1, final Expense expense2) {
        return expense1.getValue().compareTo(expense2.getValue());
    }
}
