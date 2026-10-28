package ro.pandemonium.expense.view;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.StringRes;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.ExpenseType;

public final class ExpenseTypeUi {

    private ExpenseTypeUi() {
    }

    @StringRes
    public static int getTextResource(final ExpenseType expenseType) {
        return switch (expenseType) {
            case OTHER -> R.string.OTHER;
            case FOOD -> R.string.FOOD;
            case RESTAURANT -> R.string.RESTAURANT;
            case PETROL -> R.string.PETROL;
            case TAXES -> R.string.TAXES;
            case GAS -> R.string.GAS;
            case ELECTRICITY -> R.string.ELECTRICITY;
            case INTERNET -> R.string.INTERNET;
            case PHONE -> R.string.PHONE;
            case GYM -> R.string.GYM;
            case CAR -> R.string.CAR;
            case SPECIAL -> R.string.SPECIAL;
        };
    }

    @ColorInt
    public static int getColor(final ExpenseType expenseType) {
        return switch (expenseType) {
            case OTHER -> Color.BLUE;
            case FOOD -> Color.WHITE;
            case RESTAURANT -> Color.rgb(239, 228, 176);
            case PETROL -> Color.rgb(90, 83, 67);
            case TAXES -> Color.rgb(239, 21, 10);
            case GAS -> Color.rgb(97, 73, 3);
            case ELECTRICITY -> Color.rgb(123, 186, 255);
            case INTERNET -> Color.rgb(89, 223, 26);
            case PHONE -> Color.rgb(220, 130, 29);
            case GYM -> Color.rgb(173, 179, 79);
            case CAR -> Color.rgb(233, 233, 133);
            case SPECIAL -> Color.rgb(166, 0, 83);
        };
    }
}
