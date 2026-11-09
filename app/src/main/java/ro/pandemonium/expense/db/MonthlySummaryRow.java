package ro.pandemonium.expense.db;

import androidx.room.ColumnInfo;

public final class MonthlySummaryRow {

    @ColumnInfo(name = "EXPENSE_TYPE")
    public int expenseType;

    @ColumnInfo(name = "TOTAL")
    public double total;

    @ColumnInfo(name = "MONTH")
    public String month;

}
