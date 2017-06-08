package ro.pandemonium.expense.util;

import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    private static Calendar calendar = Calendar.getInstance();

    private DateUtil() {
    }

    public static int extractYear(final Date date) {
        calendar.setTime(date);

        return calendar.get(Calendar.YEAR);
    }

    public static Date startOfYear(final int year) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, 1);

        return calendar.getTime();
    }

    public static Date endOfYear(final int year) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, 11);
        calendar.set(Calendar.DAY_OF_MONTH, 31);

        return calendar.getTime();
    }

    public static Date addYears(final Date date, final int years) {
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, years);

        return calendar.getTime();
    }

}
