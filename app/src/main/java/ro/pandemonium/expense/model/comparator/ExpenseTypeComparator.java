package ro.pandemonium.expense.model.comparator;

import ro.pandemonium.expense.model.Expense;

import java.io.Serializable;
import java.util.Comparator;

public class ExpenseTypeComparator implements Comparator<Expense>, Serializable {

    @Override
    public int compare(final Expense expense, final Expense expense2) {
        return expense.getExpenseType().compareTo(expense2.getExpenseType());
    }
}
