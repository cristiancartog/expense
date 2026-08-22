package ro.pandemonium.expense.util;

class StringUtils {

    private static final String EMPTY_STRING = "";

    static boolean isEmpty(final String string) {
        return string == null || string.trim().equals(EMPTY_STRING);
    }

}
