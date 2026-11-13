package ro.pandemonium.expense.db;

import androidx.room.ColumnInfo;

import java.math.BigDecimal;

public final class MonthlySummaryRow {

    @ColumnInfo(name = "EXPENSE_TYPE")
    public int expenseType;

    @ColumnInfo(name = "TOTAL")
    public BigDecimal total;

    @ColumnInfo(name = "MONTH")
    public String month;

}
