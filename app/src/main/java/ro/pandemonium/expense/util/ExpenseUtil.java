package ro.pandemonium.expense.util;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static ro.pandemonium.expense.util.ExpenseMathUtil.SCALE;
import static ro.pandemonium.expense.util.ExpenseMathUtil.ZERO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseUtil {

    private static final BigDecimal MAX_VALUE = new BigDecimal("999999999.99");

    public static boolean isValidValue(final BigDecimal value) {
        return value != null
                && value.signum() >= 0
                && value.compareTo(MAX_VALUE) <= 0
                && value.stripTrailingZeros().scale() <= SCALE;
    }

    public static BigDecimal computeTotalValue(final List<Expense> expenses) {
        return expenses.stream()
                       .map(Expense::getValue)
                       .reduce(ZERO, BigDecimal::add);
    }

    public static Map<ExpenseType, BigDecimal> computeExpenseSumByCategory(final List<Expense> expenses) {
        return expenses.stream()
                .collect(
                        groupingBy(
                                Expense::getExpenseType,
                                reducing(ZERO, Expense::getValue, BigDecimal::add)
                        )
                );
    }
}
