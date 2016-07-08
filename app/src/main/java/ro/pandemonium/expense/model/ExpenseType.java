package ro.pandemonium.expense.model;

import android.graphics.Color;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.comparator.ExpenseTypeOrderComparator;

public enum ExpenseType {

    OTHER(0, Integer.MAX_VALUE, Color.BLUE, 1, R.string.OTHER),
    FOOD(80, Integer.MAX_VALUE, Color.rgb(255, 255, 255), 2, R.string.FOOD),
    RESTAURANT(90, Integer.MAX_VALUE, Color.rgb(239, 228, 176), 3, R.string.RESTAURANT),
    PETROL(70, Integer.MAX_VALUE, Color.rgb(90, 83, 67), 4, R.string.PETROL),
    TAXES(50, 1, Color.rgb(239, 21, 10), 5, R.string.TAXES),
    GAS(10, 1, Color.rgb(97, 73, 3), 6, R.string.GAS),
    ELECTRICITY(20, 1, Color.rgb(123, 186, 255), 7, R.string.ELECTRICITY),
    INTERNET(40, 1, Color.rgb(89, 223, 26), 8, R.string.INTERNET),
    PHONE(30, 1, Color.rgb(220, 130, 29), 9, R.string.PHONE),
    GYM(60, 1, Color.rgb(173, 179, 79), 10, R.string.GYM),
    CAR(100, Integer.MAX_VALUE, Color.rgb(233, 233, 133), 11, R.string.CAR);

    private int maxOccurences;
    private int dbId;
    private int color;
    private int order;
    private int textResource;

    ExpenseType(final int dbId, final int monthlyOccurences, final int color, final int order, final int textResource) {
        this.dbId = dbId;
        this.maxOccurences = monthlyOccurences;
        this.color = color;
        this.order = order;
        this.textResource = textResource;
    }

    public int getMaxOccurences() {
        return maxOccurences;
    }

    public int getDbId() {
        return dbId;
    }

    public int getTextResource() {
        return textResource;
    }

    public int getColor() {
        return color;
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
