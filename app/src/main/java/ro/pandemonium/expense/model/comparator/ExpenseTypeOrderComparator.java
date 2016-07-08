package ro.pandemonium.expense.model.comparator;

import java.util.Comparator;

import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseTypeOrderComparator implements Comparator<ExpenseType> {

    @Override
    public int compare(ExpenseType lhs, ExpenseType rhs) {
        return lhs.getOrder() - rhs.getOrder();
    }
}
