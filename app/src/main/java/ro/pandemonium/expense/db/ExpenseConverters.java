package ro.pandemonium.expense.db;

import androidx.room.TypeConverter;

import java.time.LocalDate;

import ro.pandemonium.expense.model.ExpenseType;

public final class ExpenseConverters {

    private ExpenseConverters() {
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
