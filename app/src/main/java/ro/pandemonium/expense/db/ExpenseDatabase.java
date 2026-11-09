package ro.pandemonium.expense.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(
    entities = ExpenseEntity.class,
    version = 1
)
@TypeConverters(ExpenseConverters.class)
public abstract class ExpenseDatabase extends RoomDatabase {

    public static final String NAME = "expenses.db";

    public abstract ExpenseDao expenseDao();

}
