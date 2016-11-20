package ro.pandemonium.expense.view;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

import ro.pandemonium.expense.model.ExpenseType;

public class ExpenseTypeColor {

    private static Map<ExpenseType, Integer> expenseTypeToColor = new HashMap<>();
    static {
        expenseTypeToColor.put(ExpenseType.OTHER, Color.BLUE);
        expenseTypeToColor.put(ExpenseType.FOOD, Color.rgb(255, 255, 255));
        expenseTypeToColor.put(ExpenseType.RESTAURANT, Color.rgb(239, 228, 176));
        expenseTypeToColor.put(ExpenseType.PETROL, Color.rgb(90, 83, 67));
        expenseTypeToColor.put(ExpenseType.TAXES, Color.rgb(239, 21, 10));
        expenseTypeToColor.put(ExpenseType.GAS, Color.rgb(97, 73, 3));
        expenseTypeToColor.put(ExpenseType.ELECTRICITY, Color.rgb(123, 186, 255));
        expenseTypeToColor.put(ExpenseType.INTERNET, Color.rgb(89, 223, 26));
        expenseTypeToColor.put(ExpenseType.PHONE, Color.rgb(220, 130, 29));
        expenseTypeToColor.put(ExpenseType.GYM, Color.rgb(173, 179, 79));
        expenseTypeToColor.put(ExpenseType.CAR, Color.rgb(233, 233, 133));
    }

    public static int color(final ExpenseType expenseType) {
        return expenseTypeToColor.get(expenseType);
    }
}
