package ro.pandemonium.expense.view;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseTypeColor {

    private static final Map<ExpenseType, Integer> EXPENSE_TYPE_TO_COLOR = new HashMap<>();
    static {
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.OTHER, Color.BLUE);
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.FOOD, Color.rgb(255, 255, 255));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.RESTAURANT, Color.rgb(239, 228, 176));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.PETROL, Color.rgb(90, 83, 67));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.TAXES, Color.rgb(239, 21, 10));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.GAS, Color.rgb(97, 73, 3));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.ELECTRICITY, Color.rgb(123, 186, 255));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.INTERNET, Color.rgb(89, 223, 26));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.PHONE, Color.rgb(220, 130, 29));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.GYM, Color.rgb(173, 179, 79));
        EXPENSE_TYPE_TO_COLOR.put(ExpenseType.CAR, Color.rgb(233, 233, 133));
    }

    public static int color(final ExpenseType expenseType) {
        return EXPENSE_TYPE_TO_COLOR.get(expenseType);
    }
}
