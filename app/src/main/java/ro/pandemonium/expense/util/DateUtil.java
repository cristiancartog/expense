package ro.pandemonium.expense.util;

import java.time.LocalDate;
import java.time.Month;

public class DateUtil {

    private DateUtil() {
    }

    public static int currentYear() {
        return LocalDate.now().getYear();
    }

    public static synchronized int year(final LocalDate date) {
        return date.getYear();
    }

    public static synchronized LocalDate startOfYear(final int year) {
        return LocalDate.of(year, Month.JANUARY, 1);
    }

    public static synchronized LocalDate endOfYear(final int year) {
        return LocalDate.of(year, Month.DECEMBER, 31);
    }

    public static synchronized LocalDate addYears(final LocalDate date, final int years) {
        return date.plusYears(years);
    }

}
