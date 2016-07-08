package ro.pandemonium.expense;

public class Constants {

    public static final String APPLICATION_NAME = "EXPENSES";

    // formatters
    public static final String NUMBER_FORMAT_PATTERN = "###,###.##";
    public static final String ADD_ACTIVITY_FORMAT_PATTERN = "#.##";
    public static final String DATE_FORMAT_PATTERN_DB = "yyyy-MM-dd";
    public static final String DATE_FORMAT_PATTERN_FILE_TIMESTAMP = "yyyy-MM-dd--HH-mm-ss";
    public static final String DATE_FORMAT_PATTERN_DISPLAY = "dd MMM yyyy";
    public static final String DATE_FORMAT_PATTERN_MONTH = "MMM yyyy";

    // intent parameters
    public static final String INTENT_EXPENSE_COUNT_MAP = "expenseCountMap";
    public static final String INTENT_EXPENSE_EXPENSE_TO_EDIT = "expenseToEdit";
    public static final String INTENT_FILTERS = "filters";
    public static final String INTENT_EXPENSE = "expense";
    public static final String INTENT_CHANGED_EXPENSES = "changedExpenses";

    // chart intent parameters
    public static final String INTENT_EXPENSE_VALUES_BY_TYPE = "expenseValuesByType";

}
