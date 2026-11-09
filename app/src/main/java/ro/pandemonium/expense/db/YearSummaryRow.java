package ro.pandemonium.expense.db;

import androidx.room.ColumnInfo;

public final class YearSummaryRow {

    @ColumnInfo(name = "TOTAL")
    public double total;

    @ColumnInfo(name = "MONTH")
    public String month;

}
