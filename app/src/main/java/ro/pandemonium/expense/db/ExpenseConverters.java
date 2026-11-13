package ro.pandemonium.expense.db;

import static ro.pandemonium.expense.util.ExpenseMathUtil.SCALE;

import androidx.room.TypeConverter;

import java.math.BigDecimal;
import java.time.LocalDate;

import ro.pandemonium.expense.model.ExpenseType;

public final class ExpenseConverters {

    private ExpenseConverters() {
    }

    // Room calls these converters from generated DAO code. The integer represents
    // value * 10^SCALE (e.g. 12.50 -> 1250), independently of the currency.
    @TypeConverter
    public static Long valueToScaledInteger(final BigDecimal value) {
        return value == null ? null : value.movePointRight(SCALE).longValueExact();
    }

    @TypeConverter
    public static BigDecimal scaledIntegerToValue(final Long scaledValue) {
        return scaledValue == null ? null : BigDecimal.valueOf(scaledValue, SCALE);
    }

    @TypeConverter
    public static Integer expenseTypeToDbId(final ExpenseType expenseType) {
        return expenseType == null ? null : expenseType.getDbId();
    }

    @TypeConverter
    public static ExpenseType dbIdToExpenseType(final Integer dbId) {
        return dbId == null ? null : ExpenseType.byDbId(dbId);
    }

    @TypeConverter
    public static String localDateToText(final LocalDate date) {
        return date == null ? null : date.toString();
    }

    @TypeConverter
    public static LocalDate textToLocalDate(final String date) {
        return date == null ? null : LocalDate.parse(date);
    }
}
