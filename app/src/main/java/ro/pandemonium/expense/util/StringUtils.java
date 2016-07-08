package ro.pandemonium.expense.util;

public class StringUtils {

    private static final String EMPTY_STRING = "";

    public static boolean isEmpty(final String string) {
        return string == null || string.equals(EMPTY_STRING);
    }

}
