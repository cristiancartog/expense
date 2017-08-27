package ro.pandemonium.expense.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    private static final Calendar CALENDAR = Calendar.getInstance();

    private DateUtil() {
    }

    public static int currentYear() {
        return year(new Date());
    }

//    public static int currentMonth() {
//        return monthOfYear(new Date());
//    }

    public static synchronized int year(final Date date) {
        CALENDAR.setTime(date);

        return CALENDAR.get(Calendar.YEAR);
    }

//    private static synchronized int monthOfYear(final Date date) {
//        CALENDAR.setTime(date);
//
//        return CALENDAR.get(Calendar.MONTH);
//    }

    public static synchronized Date startOfYear(final int year) {
        CALENDAR.set(Calendar.YEAR, year);
        CALENDAR.set(Calendar.DAY_OF_YEAR, 1);

        return CALENDAR.getTime();
    }

    public static synchronized Date endOfYear(final int year) {
        CALENDAR.set(Calendar.YEAR, year);
        CALENDAR.set(Calendar.MONTH, 11);
        CALENDAR.set(Calendar.DAY_OF_MONTH, 31);

        return CALENDAR.getTime();
    }

    public static synchronized Date addYears(final Date date, final int years) {
        CALENDAR.setTime(date);
        CALENDAR.add(Calendar.YEAR, years);

        return CALENDAR.getTime();
    }

}
