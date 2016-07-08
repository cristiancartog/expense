package ro.pandemonium.expense.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseUtil {

    public static double computeTotalValue(final List<Expense> expenses) {
        double total = 0;

        for (Expense expense : expenses) {
            total += expense.getValue();
        }

        return total;
    }

    public static Map<ExpenseType, Double> computeExpenseSumByCategory(final List<Expense> expenses) {
        final Map<ExpenseType, Double> results = new HashMap<>();

        for (Expense expense : expenses) {
            ExpenseType expenseType = expense.getExpenseType();
            Double value = results.get(expenseType);
            if (value == null) {
                value = expense.getValue();
                results.put(expenseType, value);
            } else {
                results.put(expenseType, value + expense.getValue());
            }
        }

        return results;
    }
}
