package ro.pandemonium.expense.util;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseUtil {

    private static final double MAX_VALUE = 999999999.99D;

    public static boolean isValidValue(final Double value) {
        return value != null
                && Double.isFinite(value)
                && value >= 0D
                && value <= MAX_VALUE
                && value == Math.round(value * 100D) / 100D;
    }

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
