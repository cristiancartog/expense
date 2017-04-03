package ro.pandemonium.expense.model;

public class MonthWrapper {

    private int year;
    private int month;

    public MonthWrapper(final int year, final int month) {
        this.year = year;
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }
}
