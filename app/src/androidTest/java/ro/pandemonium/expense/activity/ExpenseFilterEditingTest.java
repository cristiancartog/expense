package ro.pandemonium.expense.activity;

import static android.app.Activity.RESULT_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;
import static ro.pandemonium.expense.activity.ExpenseActivityTestSupport.awaitExpensesLoaded;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
public class ExpenseFilterEditingTest {

    private static final YearMonth MONTH = YearMonth.of(2000, 1);

    @Parameterized.Parameters(name = "{0}, local {1}")
    public static List<Object[]> criteria() {
        final List<Object[]> criteria = new ArrayList<>();
        for (String screen : List.of("main", "search type", "search comment")) {
            for (String localFilter : List.of("type", "comment")) {
                criteria.add(new Object[]{screen, localFilter});
            }
        }
        return criteria;
    }

    @Parameterized.Parameter
    public String screen;
    @Parameterized.Parameter(1)
    public String localFilter;

    private ExpenseDatabase database;
    private ActivityScenario<? extends AbstractExpenseListActivity> scenario;
    private long firstId;
    private long secondId;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase.class)
                       .allowMainThreadQueries()
                       .build();
        firstId = insert(FOOD, "10.00", "global local");
        secondId = insert(PETROL, "20.00", "global other");
        insert(SPECIAL, "30.00", "outside");

        if (screen.equals("main")) {
            final ActivityScenario<ExpenseMainActivity> mainScenario = ActivityScenario.launch(ExpenseMainActivity.class);
            scenario = mainScenario;
            awaitExpensesLoaded(scenario);
            mainScenario.onActivity(activity -> {
                activity.mainService = new ExpenseMainService(database.expenseDao(), context);
                activity.mainService.initialize();
                activity.onDateSet(null, MONTH.getYear(), MONTH.getMonthValue() - 1, 1);
            });
            awaitMainMonth();
        } else {
            final Intent intent = new Intent(context, ExpenseSearchResultActivity.class);
            intent.putExtra(INTENT_FILTERS, new Filters(List.of(), Set.of()));
            final ActivityScenario<ExpenseSearchResultActivity> searchScenario = ActivityScenario.launch(intent);
            scenario = searchScenario;
            awaitExpensesLoaded(scenario);
            searchScenario.onActivity(activity -> {
                activity.searchService = new ExpenseSearchService(database.expenseDao());
                activity.expenseListAdapter.updateExpenseList(activity.searchService.initialize(globalFilters()));
            });
        }
        scenario.onActivity(activity -> {
            activity.expenseListAdapter.filterBy(localFilters());
            activity.renderExpenses();
            assertRows(activity, List.of(firstId), 2, "10.00", "30.00");
        });
    }

    private long insert(final ExpenseType type, final String value, final String comment) {
        final ExpenseEntity entity = new ExpenseEntity();
        entity.expenseType = type;
        entity.value = new BigDecimal(value);
        entity.date = MONTH.atDay(10);
        entity.comment = comment;
        return database.expenseDao().insert(entity);
    }

    private Filters globalFilters() {
        return screen.equals("search comment")
                ? new Filters(List.of(), Set.of("GLOBAL"))
                : new Filters(List.of(FOOD, PETROL), Set.of());
    }

    private Filters localFilters() {
        return localFilter.equals("type")
                ? new Filters(List.of(FOOD), Set.of())
                : new Filters(List.of(), Set.of("local"));
    }

    private void awaitMainMonth() {
        final String expected = MONTH.format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault()));
        final AtomicBoolean loaded = new AtomicBoolean();
        final long deadline = SystemClock.uptimeMillis() + 5_000;
        while (!loaded.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                final TextView monthButton = activity.findViewById(R.id.expenseListChangeMonthButton);
                loaded.set(expected.contentEquals(monthButton.getText()));
            });
            if (!loaded.get()) {
                SystemClock.sleep(20);
            }
        }
        assertTrue("Selected month did not load", loaded.get());
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void successfulEditsReapplyLocalAndGlobalMembership() {
        scenario.onActivity(activity -> {
            activity.expenseListAdapter.sortExpenses(Comparator.comparing(Expense::getValue).reversed());
            edit(activity, firstId, PETROL, "global other", "15.00");
            assertRows(activity, List.of(), 2, "0.00", "35.00");

            // A result for a row hidden by the local filter must be able to make it visible again.
            edit(activity, secondId, FOOD, "global local", "25.00");
            assertRows(activity, List.of(secondId), 2, "25.00", "40.00");

            // Still matches the local criteria, but no longer belongs to the underlying query.
            final ExpenseType editedType = screen.equals("search type") ? SPECIAL : FOOD;
            final String editedComment = screen.equals("search comment") ? "local outside" : "global local";
            if (activity instanceof ExpenseSearchResultActivity search) {
                edit(search, secondId, editedType, editedComment, "26.00");
                assertRows(search, List.of(), 1, "0.00", "15.00");
                edit(search, secondId, FOOD, "global local", "27.00");
                assertRows(search, List.of(secondId), 2, "27.00", "42.00");
                assertEquals(4, search.searchService.changedExpenses().size());
            }

            activity.clearFilters();
            assertEquals(2, activity.expenseListAdapter.getCount());
            assertEquals(secondId, ((Expense) activity.expenseListAdapter.getItem(0)).getId().longValue());
            assertEquals(PETROL, activity.expenseListAdapter.getExpenses(true).stream()
                                                         .filter(expense -> expense.getId() == firstId)
                                                         .findFirst()
                                                         .orElseThrow()
                                                         .getExpenseType());
        });
    }

    @Test
    public void returningFromSearchReappliesTheMainFilter() {
        assumeTrue(screen.equals("main"));
        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(ExpenseSearchResultActivity.class.getName(), null, false);
        final AtomicLong newExpenseId = new AtomicLong();
        try {
            scenario.onActivity(activity -> ((ExpenseMainActivity) activity).recurrentExpensesRequested(null));
            final ExpenseSearchResultActivity search = (ExpenseSearchResultActivity) monitor.waitForActivityWithTimeout(5_000);
            assertNotNull("Search results did not open", search);
            awaitExpensesLoaded(search);
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                try {
                    search.searchService = new ExpenseSearchService(database.expenseDao());
                    search.expenseListAdapter.updateExpenseList(search.searchService.initialize(globalFilters()));
                    edit(search, firstId, PETROL, "global other", "15.00");
                    edit(search, secondId, FOOD, "global local", "25.00");
                    // An expense newly included in the main list must also preserve its filter.
                    final long newId = insert(FOOD, "5.00", "global local");
                    newExpenseId.set(newId);
                    edit(search, newId, FOOD, "global local", "5.00");
                } finally {
                    search.finish();
                }
            });
            instrumentation.waitForIdleSync();
            scenario.onActivity(activity -> {
                assertRows(activity, List.of(secondId, newExpenseId.get()), 3, "30.00", "45.00");
                activity.clearFilters();
                assertEquals(3, activity.expenseListAdapter.getCount());
            });
        } finally {
            instrumentation.removeMonitor(monitor);
        }
    }

    @Test
    public void typeAndCommentFiltersRetainTheirUnionAfterEdits() {
        scenario.onActivity(activity -> {
            activity.expenseListAdapter.filterBy(new Filters(List.of(FOOD), Set.of("local")));
            edit(activity, firstId, PETROL, "global local", "15.00");
            assertRows(activity, List.of(firstId), 2, "15.00", "35.00");
            edit(activity, secondId, FOOD, "global other", "25.00");
            assertRows(activity, List.of(firstId, secondId), 2, "40.00", "40.00");
            edit(activity, firstId, PETROL, "global other", "15.00");
            assertRows(activity, List.of(secondId), 2, "25.00", "40.00");
        });
    }

    private void edit(final AbstractExpenseListActivity activity, final long id, final ExpenseType type, final String comment, final String value) {
        final Expense expense = new Expense();
        expense.setId(id);
        expense.setExpenseType(type);
        expense.setDate(MONTH.atDay(15));
        expense.setComment(comment);
        expense.setValue(new BigDecimal(value));
        activity.processAddEditExpenseResult(RESULT_OK, new Intent().putExtra(INTENT_EXPENSE, expense));
    }

    private void assertRows(final AbstractExpenseListActivity activity, final List<Long> visibleIds, final int sourceSize, final String visibleTotal, final String sourceTotal) {
        assertTrue(activity.expenseListAdapter.isFiltered());
        assertEquals(visibleIds, activity.expenseListAdapter.getExpenses(false).stream()
                                                        .map(Expense::getId)
                                                        .sorted()
                                                        .toList());
        assertEquals(sourceSize, activity.expenseListAdapter.getExpenses(true).size());
        final var formatter = ExpenseFormatUtil.amountFormatter();
        final BigDecimal filtered = new BigDecimal(visibleTotal);
        final BigDecimal total = new BigDecimal(sourceTotal);
        final BigDecimal percentage = ro.pandemonium.expense.util.ExpenseMathUtil.percentage(filtered, total);
        assertEquals(
                activity.getString(R.string.abstractExpenseListActivityFilteredTotalLabel, formatter.format(filtered), formatter.format(percentage), formatter.format(total)),
                activity.totalTextView.getText().toString()
        );
        if (activity instanceof ExpenseMainActivity) {
            final PieChart chart = activity.findViewById(R.id.expenseListPieChart);
            assertEquals(filtered.floatValue(), chart.getData().getYValueSum(), 0.001F);
        }
    }
}
