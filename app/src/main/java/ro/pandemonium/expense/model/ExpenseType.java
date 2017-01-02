package ro.pandemonium.expense.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.comparator.ExpenseTypeOrderComparator;

public enum ExpenseType {

    OTHER(0, Integer.MAX_VALUE, 1, R.string.OTHER),
    FOOD(80, Integer.MAX_VALUE, 2, R.string.FOOD),
    RESTAURANT(90, Integer.MAX_VALUE, 3, R.string.RESTAURANT),
    PETROL(70, Integer.MAX_VALUE, 4, R.string.PETROL),
    TAXES(50, 1, 5, R.string.TAXES),
    GAS(10, 1, 6, R.string.GAS),
    ELECTRICITY(20, 1, 7, R.string.ELECTRICITY),
    INTERNET(40, 1, 8, R.string.INTERNET),
    PHONE(30, 1, 9, R.string.PHONE),
    GYM(60, 1, 10, R.string.GYM),
    CAR(100, Integer.MAX_VALUE, 11, R.string.CAR),
    SPECIAL(999, Integer.MAX_VALUE, 12, R.string.SPECIAL);

    private final int maxOccurrences;
    private final int dbId;
    private final int order;
    private final int textResource;

    ExpenseType(final int dbId, final int monthlyOccurrences, final int order, final int textResource) {
        this.dbId = dbId;
        this.maxOccurrences = monthlyOccurrences;
        this.order = order;
        this.textResource = textResource;
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

    public static ExpenseType forDbId(final int dbId) {
        ExpenseType result = ExpenseType.OTHER;

        for (ExpenseType expenseType : ExpenseType.values()) {
            if (expenseType.getDbId() == dbId) {
                result = expenseType;
                break;
            }
        }

        return result;
    }

    public static List<ExpenseType> orderedExpenseTypes() {
        List<ExpenseType> expenseTypes = Arrays.asList(ExpenseType.values());

        Collections.sort(expenseTypes, new ExpenseTypeOrderComparator());

        return expenseTypes;
    }
}
