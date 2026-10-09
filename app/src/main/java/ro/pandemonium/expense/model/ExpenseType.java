package ro.pandemonium.expense.model;

import static java.util.stream.Collectors.toMap;

import android.graphics.Color;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import ro.pandemonium.expense.R;

public enum ExpenseType {

    OTHER(0, Integer.MAX_VALUE, 1, R.string.OTHER, Color.BLUE),
    FOOD(80, Integer.MAX_VALUE, 2, R.string.FOOD, Color.WHITE),
    RESTAURANT(90, Integer.MAX_VALUE, 3, R.string.RESTAURANT, Color.rgb(239, 228, 176)),
    PETROL(70, Integer.MAX_VALUE, 4, R.string.PETROL, Color.rgb(90, 83, 67)),
    TAXES(50, 1, 5, R.string.TAXES, Color.rgb(239, 21, 10)),
    GAS(10, 1, 6, R.string.GAS, Color.rgb(97, 73, 3)),
    ELECTRICITY(20, 1, 7, R.string.ELECTRICITY, Color.rgb(123, 186, 255)),
    INTERNET(40, 1, 8, R.string.INTERNET, Color.rgb(89, 223, 26)),
    PHONE(30, 1, 9, R.string.PHONE, Color.rgb(220, 130, 29)),
    GYM(60, 1, 10, R.string.GYM, Color.rgb(173, 179, 79)),
    CAR(100, Integer.MAX_VALUE, 11, R.string.CAR, Color.rgb(233, 233, 133)),
    SPECIAL(999, Integer.MAX_VALUE, 12, R.string.SPECIAL, Color.rgb(166, 0, 83));

    private static final Map<Integer, ExpenseType> MAP_DB_ID_TO_EXPENSE_TYPE = Stream.of(values())
            .collect(toMap(
                    ExpenseType::getDbId,
                    Function.identity()
            ));


    private final int maxOccurrences;
    private final int dbId;
    private final int order;
    private final int textResource;
    private final int color;

    ExpenseType(final int dbId, final int monthlyOccurrences, final int order, final int textResource, final int color) {
        this.dbId = dbId;
        this.maxOccurrences = monthlyOccurrences;
        this.order = order;
        this.textResource = textResource;
        this.color = color;
    }

    public int getMaxOccurrences() {
        return maxOccurrences;
    }

    public int getDbId() {
        return dbId;
    }

    public int getTextResource() {
        return textResource;
    }

    public int getOrder() {
        return order;
    }

    public int getColor() {
        return color;
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
