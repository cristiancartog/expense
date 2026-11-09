package ro.pandemonium.expense.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

import ro.pandemonium.expense.model.ExpenseType;

@Entity(
    tableName = "EXPENSES",
    indices = @Index(name = "IDX_EXPENSE_DATE", value = "DATE")
)
public final class ExpenseEntity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_ID")
    public Long id;

    @NonNull
    @ColumnInfo(name = "EXPENSE_TYPE")
    public ExpenseType expenseType;

    @NonNull
    @ColumnInfo(name = "VALUE")
    public Double value;

    @NonNull
    @ColumnInfo(name = "DATE")
    public LocalDate date;

    @ColumnInfo(name = "COMMENT")
    public String comment;

}
