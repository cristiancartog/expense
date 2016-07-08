package ro.pandemonium.expense.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Filters implements Serializable {

    private List<ExpenseType> expenseTypes;
    private Set<String> comments;

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

    public static Filters defaultFilters() {
        Arrays.asList(ExpenseType.values());
        return new Filters(Arrays.asList(ExpenseType.values()), new HashSet<String>());
    }
}
