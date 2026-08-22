package ro.pandemonium.expense.model;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public record Filters(List<ExpenseType> expenseTypes,
                      Set<String> comments) implements Serializable {

    public boolean isSelectionEmpty() {
        return expenseTypes.isEmpty() && comments.isEmpty();

    }
}
