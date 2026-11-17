package ro.pandemonium.expense.activity;

import static android.app.Activity.RESULT_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;
import static ro.pandemonium.expense.activity.ExpenseActivityTestSupport.awaitExpensesLoaded;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.mikephil.charting.charts.PieChart;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDatabase;
import ro.pandemonium.expense.db.ExpenseEntity;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.service.ExpenseMainService;
import ro.pandemonium.expense.service.ExpenseSearchService;
import ro.pandemonium.expense.util.ExpenseFormatUtil;

@RunWith(Parameterized.class)
public class ExpenseCategoryShortcutTest {

    private static final YearMonth SELECTED_MONTH = YearMonth.of(2000, 1);

    @Parameterized.Parameters(name = "{0}")
    public static List<String> shortcuts() {
        return List.of("special", "subscription", "recurrent");
    }

    @Parameterized.Parameter
    public String shortcut;

    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private ExpenseDatabase database;
    private ActivityScenario<ExpenseMainActivity> scenario;
    private Instrumentation.ActivityMonitor searchMonitor;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase.class)
                       .allowMainThreadQueries()
                       .build();
        scenario = ActivityScenario.launch(ExpenseMainActivity.class);
        awaitExpensesLoaded(scenario);
        scenario.onActivity(activity -> {
            // Keep the application's installed data untouched; both screens use this test database.
            activity.mainService = new ExpenseMainService(database.expenseDao(), context);
            activity.mainService.initialize();
            activity.onDateSet(null, SELECTED_MONTH.getYear(), SELECTED_MONTH.getMonthValue() - 1, 1);
        });
        final String monthLabel = SELECTED_MONTH.format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault()));
        final AtomicBoolean loaded = new AtomicBoolean();
        final long deadline = SystemClock.uptimeMillis() + 5_000;
        while (!loaded.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                final TextView monthButton = activity.findViewById(R.id.expenseListChangeMonthButton);
                loaded.set(monthLabel.contentEquals(monthButton.getText()));
            });
            if (!loaded.get()) {
                SystemClock.sleep(20);
            }
        }
        assertTrue("Selected month did not load", loaded.get());
        searchMonitor = instrumentation.addMonitor(ExpenseSearchResultActivity.class.getName(), null, false);
    }

    @After
    public void tearDown() {
        if (searchMonitor != null) {
            instrumentation.removeMonitor(searchMonitor);
        }
        if (scenario != null) {
            scenario.close();
        }
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void amountEditsRefreshTheMainListTotalAndChart() {
        verifyReturn(SELECTED_MONTH, shortcutType(), SELECTED_MONTH, shortcutType(), false);
    }

    @Test
    public void categoryChangesRefreshTheMainListTotalAndChart() {
        verifyReturn(SELECTED_MONTH, shortcutType(), SELECTED_MONTH, FOOD, false);
    }

    @Test
    public void becomingSpecialRemovesTheExpenseFromTheMainScreen() {
        verifyReturn(SELECTED_MONTH, shortcutType(), SELECTED_MONTH, SPECIAL, false);
    }

    @Test
    public void movingOutOfTheSelectedMonthRefreshesTheMainScreen() {
        verifyReturn(SELECTED_MONTH, shortcutType(), SELECTED_MONTH.plusMonths(1), shortcutType(), false);
    }

    @Test
    public void movingIntoTheSelectedMonthRefreshesTheMainScreen() {
        verifyReturn(SELECTED_MONTH.minusMonths(1), shortcutType(), SELECTED_MONTH, shortcutType(), false);
    }

    @Test
    public void deletionsRefreshTheMainListTotalAndChart() {
        verifyReturn(SELECTED_MONTH, shortcutType(), SELECTED_MONTH, shortcutType(), true);
    }

    private ExpenseType shortcutType() {
        return shortcutTypes().getFirst();
    }

    private List<ExpenseType> shortcutTypes() {
        return switch (shortcut) {
            case "special" -> List.of(SPECIAL);
            case "subscription" -> ExpenseType.subscriptionExpenses();
            case "recurrent" -> ExpenseType.recurrentExpenses();
            default -> throw new IllegalArgumentException(shortcut);
        };
    }

    private void verifyReturn(final YearMonth originalMonth,
                              final ExpenseType originalType,
                              final YearMonth editedMonth,
                              final ExpenseType editedType,
                              final boolean delete) {
        final ExpenseEntity original = new ExpenseEntity();
        original.expenseType = originalType;
        original.date = originalMonth.atDay(10);
        original.value = new BigDecimal("10.00");
        original.comment = "shortcut test";
        original.id = database.expenseDao().insert(original);
        scenario.onActivity(activity -> {
            activity.expenseListAdapter.updateExpenseList(activity.mainService.reload().expenses());
            activity.renderExpenses();
            switch (shortcut) {
                case "special" -> activity.specialExpensesRequested(null);
                case "subscription" -> activity.subscriptionExpensesRequested(null);
                case "recurrent" -> activity.recurrentExpensesRequested(null);
                default -> throw new IllegalArgumentException(shortcut);
            }
        });
        final ExpenseSearchResultActivity search = (ExpenseSearchResultActivity) searchMonitor.waitForActivityWithTimeout(5_000);
        assertNotNull("Shortcut did not open search results", search);
        awaitExpensesLoaded(search);
        instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(() -> {
            try {
                final Filters filters = search.getIntent().getSerializableExtra(INTENT_FILTERS, Filters.class);
                assertEquals(new Filters(shortcutTypes(), Set.of()), filters);
                search.searchService = new ExpenseSearchService(database.expenseDao());
                search.expenseListAdapter.updateExpenseList(search.searchService.initialize(filters));
                final Expense expense = (Expense) search.expenseListAdapter.getItem(0);
                if (delete) {
                    search.deleteExpense(expense.getId());
                } else {
                    expense.setDate(editedMonth.atDay(15));
                    expense.setExpenseType(editedType);
                    expense.setValue(new BigDecimal("25.00"));
                    expense.setComment("edited through " + shortcut);
                    search.processAddEditExpenseResult(RESULT_OK, new Intent().putExtra(INTENT_EXPENSE, expense));
                }
            } finally {
                search.finish();
            }
        });
        instrumentation.waitForIdleSync();
        scenario.onActivity(activity -> {
            final boolean visible = !delete && editedType != SPECIAL && SELECTED_MONTH.equals(editedMonth);
            assertEquals(visible ? 1 : 0, activity.expenseListAdapter.getCount());
            final BigDecimal expectedTotal = new BigDecimal(visible ? "25.00" : "0.00");
            assertEquals(
                    ExpenseFormatUtil.amountFormatter().format(expectedTotal),
                    activity.totalTextView.getText().toString()
            );
            final PieChart chart = activity.findViewById(R.id.expenseListPieChart);
            assertEquals(expectedTotal.floatValue(), chart.getData().getYValueSum(), 0.001f);
            if (visible) {
                final Expense displayed = (Expense) activity.expenseListAdapter.getItem(0);
                assertEquals(original.id, displayed.getId());
                assertEquals(editedType, displayed.getExpenseType());
                assertEquals(editedMonth.atDay(15), displayed.getDate());
                assertEquals("edited through " + shortcut, displayed.getComment());
            }
        });
    }
}
