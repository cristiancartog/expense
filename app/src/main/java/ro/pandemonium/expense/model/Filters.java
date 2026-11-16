package ro.pandemonium.expense.model;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public record Filters(List<ExpenseType> expenseTypes,
                      Set<String> comments) implements Serializable {

    public static final Filters EMPTY_FILTERS = new Filters(List.of(), Set.of());

    public boolean isSelectionEmpty() {
        return expenseTypes.isEmpty() && comments.isEmpty();
    }

    public Filters copy() {
        return new Filters(List.copyOf(expenseTypes), Set.copyOf(comments));
    }
}
