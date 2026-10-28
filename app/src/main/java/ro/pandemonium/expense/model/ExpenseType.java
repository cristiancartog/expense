package ro.pandemonium.expense.model;

import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public enum ExpenseType {

    OTHER(0, Integer.MAX_VALUE, 1),
    FOOD(80, Integer.MAX_VALUE, 2),
    RESTAURANT(90, Integer.MAX_VALUE, 3),
    PETROL(70, Integer.MAX_VALUE, 4),
    TAXES(50, 1, 5),
    GAS(10, 1, 6),
    ELECTRICITY(20, 1, 7),
    INTERNET(40, 1, 8),
    PHONE(30, 1, 9),
    GYM(60, 1, 10),
    CAR(100, Integer.MAX_VALUE, 11),
    SPECIAL(999, Integer.MAX_VALUE, 12);

    private static final Map<Integer, ExpenseType> MAP_DB_ID_TO_EXPENSE_TYPE = Stream.of(values())
            .collect(toMap(
                    ExpenseType::getDbId,
                    Function.identity()
            ));


    private final int maxOccurrences;
    private final int dbId;
    private final int order;

    ExpenseType(final int dbId, final int monthlyOccurrences, final int order) {
        this.dbId = dbId;
        this.maxOccurrences = monthlyOccurrences;
        this.order = order;
    }

    public int getMaxOccurrences() {
        return maxOccurrences;
    }

    public int getDbId() {
        return dbId;
    }

    public int getOrder() {
        return order;
    }

    public static ExpenseType byDbId(final int dbId) {
        return MAP_DB_ID_TO_EXPENSE_TYPE.get(dbId);
    }

    public static List<ExpenseType> orderedExpenseTypes() {
        return Arrays.stream(ExpenseType.values())
                .sorted(Comparator.comparing(ExpenseType::getOrder))
                .toList();
    }

    public static List<ExpenseType> subscriptionExpenses() {
        return Arrays.stream(ExpenseType.values())
                .filter(expenseType -> expenseType.getMaxOccurrences() == 1)
                .toList();
    }

    public static List<ExpenseType> recurrentExpenses() {
        return Arrays.stream(ExpenseType.values())
                .filter(expenseType -> expenseType.getMaxOccurrences() > 1 && expenseType != SPECIAL)
                .toList();
    }
}
