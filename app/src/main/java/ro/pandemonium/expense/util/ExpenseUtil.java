package ro.pandemonium.expense.util;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseUtil {

    public static double computeTotalValue(final List<Expense> expenses) {
        return expenses.stream()
                .mapToDouble(Expense::getValue)
                .sum();
    }

    public static Map<ExpenseType, Double> computeExpenseSumByCategory(final List<Expense> expenses) {
        return expenses.stream()
                .collect(
                        groupingBy(
                                Expense::getExpenseType,
                                Collectors.summingDouble(Expense::getValue)
                        )
                );
    }
}
