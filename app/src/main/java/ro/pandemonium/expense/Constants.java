package ro.pandemonium.expense;

public class Constants {

    public static final String APPLICATION_NAME = "EXPENSES";

    // formatter(s)
    public static final String DATE_FORMAT_PATTERN_FILE_TIMESTAMP = "yyyy-MM-dd--HH-mm-ss";
    public static final String DATE_FORMAT_PATTERN_DISPLAY = "dd MMM yyyy";
    public static final String DATE_FORMAT_PATTERN_MONTH = "MMM yyyy";

    // intent parameters
    public static final String INTENT_EXPENSE_COUNT_MAP = "expenseCountMap";
    public static final String INTENT_EXPENSE_EXPENSE_TO_EDIT = "expenseToEdit";
    public static final String INTENT_FILTERS = "filters";
    public static final String INTENT_EXPENSE = "expense";
    public static final String INTENT_CHANGED_EXPENSES = "changedExpenses";
    public static final String INTENT_YEAR = "year";
    public static final String INTENT_EXPENSE_TYPE = "expenseType";
}
