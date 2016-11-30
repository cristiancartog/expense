package ro.pandemonium.expense.model.comparator;

import java.io.Serializable;
import java.util.Comparator;

import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseTypeOrderComparator implements Comparator<ExpenseType>, Serializable {

    @Override
    public int compare(ExpenseType lhs, ExpenseType rhs) {
        return lhs.getOrder() - rhs.getOrder();
    }
}
