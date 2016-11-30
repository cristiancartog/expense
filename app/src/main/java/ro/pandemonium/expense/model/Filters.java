package ro.pandemonium.expense.model;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public class Filters implements Serializable {

    private final List<ExpenseType> expenseTypes;
    private final Set<String> comments;

    public Filters(List<ExpenseType> expenseTypes, Set<String> comments) {
        this.expenseTypes = expenseTypes;
        this.comments = comments;
    }

    public List<ExpenseType> getExpenseTypes() {
        return expenseTypes;
    }

    public Set<String> getComments() {
        return comments;
    }

    public boolean isSelectionEmpty() {
        return expenseTypes.isEmpty() && comments.isEmpty();

    }
}
