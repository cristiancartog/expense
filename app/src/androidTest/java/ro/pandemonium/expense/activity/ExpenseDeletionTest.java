package ro.pandemonium.expense.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.github.mikephil.charting.charts.PieChart;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
public class ExpenseDeletionTest {

    private static final YearMonth MONTH = YearMonth.of(2000, 1);
    private static final Filters FILTERS = new Filters(List.of(FOOD, PETROL), Set.of());

    @Parameterized.Parameters(name = "{0}")
    public static List<String> screens() {
        return List.of("main", "search");
    }

    @Parameterized.Parameter
    public String screen;

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
        firstId = insert(FOOD, "10.00");
        secondId = insert(PETROL, "20.00");

        if (screen.equals("main")) {
            final ActivityScenario<ExpenseMainActivity> mainScenario = ActivityScenario.launch(ExpenseMainActivity.class);
            scenario = mainScenario;
            mainScenario.onActivity(activity -> {
                activity.mainService = new ExpenseMainService(database.expenseDao(), context);
                activity.mainService.initialize();
                activity.onDateSet(null, MONTH.getYear(), MONTH.getMonthValue() - 1, 1);
            });
            awaitMainMonth();
        } else {
            final Intent intent = new Intent(context, ExpenseSearchResultActivity.class);
            // Avoid loading any installed data before replacing the service with the test database.
            intent.putExtra(INTENT_FILTERS, new Filters(List.of(), Set.of()));
            final ActivityScenario<ExpenseSearchResultActivity> searchScenario = ActivityScenario.launch(intent);
            scenario = searchScenario;
            searchScenario.onActivity(activity -> {
                activity.searchService = new ExpenseSearchService(database.expenseDao());
                activity.expenseListAdapter.updateExpenseList(activity.searchService.initialize(FILTERS));
                activity.updateTotal();
            });
        }
        scenario.onActivity(activity -> {
            // Use the same row order on both screens.
            activity.expenseListAdapter.sortExpenses(Comparator.comparing(Expense::getId));
            assertEquals(2, activity.expenseListAdapter.getCount());
            assertTotal(activity, "30.00");
        });
    }

    private long insert(final ExpenseType type, final String value) {
        final ExpenseEntity entity = new ExpenseEntity();
        entity.expenseType = type;
        entity.value = new BigDecimal(value);
        entity.date = MONTH.atDay(10);
        entity.comment = "deletion test";
        return database.expenseDao().insert(entity);
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
    public void databaseFailurePreservesListTotalAndPendingSearchEdits() {
        final AtomicInteger notifications = new AtomicInteger();
        scenario.onActivity(activity -> {
            recordSearchEdit(activity);
            database.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER reject_delete BEFORE DELETE ON EXPENSES BEGIN SELECT RAISE(ABORT, 'delete failed'); END");
            observeChanges(activity, notifications);
            openConfirmation(activity);

            confirm(activity);
        });
        scenario.onActivity(activity -> {
            assertEquals(2, database.expenseDao().fetchAllExpenses().size());
            assertUnchanged(activity, notifications);
        });
    }

    @Test
    public void alreadyDeletedRowDoesNotReportSuccessOrMutateVisibleState() {
        final AtomicInteger notifications = new AtomicInteger();
        scenario.onActivity(activity -> {
            recordSearchEdit(activity);
            observeChanges(activity, notifications);
            openConfirmation(activity);
            assertEquals(1, database.expenseDao().delete(firstId));

            confirm(activity);
        });
        scenario.onActivity(activity -> {
            assertUnchanged(activity, notifications);
        });
    }

    private void recordSearchEdit(final AbstractExpenseListActivity activity) {
        if (activity instanceof ExpenseSearchResultActivity search) {
            search.searchService.update((Expense) activity.expenseListAdapter.getItem(0));
        }
    }

    private void observeChanges(final AbstractExpenseListActivity activity, final AtomicInteger notifications) {
        activity.expenseListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                notifications.incrementAndGet();
            }
        });
    }

    private View openConfirmation(final AbstractExpenseListActivity activity) {
        final View row = activity.expenseListAdapter.getView(0, null, activity.expenseList);
        final View deleteButton = row.findViewById(R.id.expenseListItemDelete);
        assertEquals(firstId, deleteButton.getTag());
        activity.deleteButtonPressed(deleteButton);
        assertEquals(2, activity.expenseListAdapter.getExpenses(true).size());
        return row;
    }

    private void confirm(final AbstractExpenseListActivity activity) {
        activity.deleteConfirmationDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
    }

    private void assertUnchanged(final AbstractExpenseListActivity activity, final AtomicInteger notifications) {
        assertEquals(0, notifications.get());
        assertEquals(2, activity.expenseListAdapter.getCount());
        assertEquals(firstId, ((Expense) activity.expenseListAdapter.getItem(0)).getId().longValue());
        assertTotal(activity, "30.00");
        if (activity instanceof ExpenseSearchResultActivity search) {
            assertEquals(1, search.searchService.changedExpenses().size());
            assertTrue(search.searchService.deletedExpenseIds().isEmpty());
        }
    }

    @Test
    public void confirmationKeepsOriginalIdWhenListAndButtonAreReplaced() {
        scenario.onActivity(activity -> {
            final View originalRow = openConfirmation(activity);
            final List<Expense> replacement = new ExpenseSearchService(database.expenseDao()).initialize(FILTERS);
            activity.expenseListAdapter.updateExpenseList(replacement);
            activity.expenseListAdapter.sortExpenses(Comparator.comparing(Expense::getId).reversed());
            activity.expenseListAdapter.getView(0, originalRow, activity.expenseList);
            assertEquals(secondId, originalRow.findViewById(R.id.expenseListItemDelete).getTag());
            activity.expenseListAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    // The adapter must notify only after the database has committed the deletion.
                    final List<Long> persistedIds = database.expenseDao().fetchAllExpenses().stream()
                                                           .map(entity -> entity.id)
                                                           .toList();
                    assertEquals(List.of(secondId), persistedIds);
                }
            });

            confirm(activity);
        });
        scenario.onActivity(activity -> {
            assertEquals(1, activity.expenseListAdapter.getCount());
            assertEquals(secondId, ((Expense) activity.expenseListAdapter.getItem(0)).getId().longValue());
            assertTotal(activity, "20.00");
            if (activity instanceof ExpenseMainActivity) {
                final PieChart chart = activity.findViewById(R.id.expenseListPieChart);
                assertEquals(20F, chart.getData().getYValueSum(), 0.001F);
            }
            if (activity instanceof ExpenseSearchResultActivity search) {
                assertEquals(List.of(firstId), search.searchService.deletedExpenseIds());
            }
        });
    }

    @Test
    public void deletionWhileFilteredRemovesTheExpenseFromTheBackupList() {
        scenario.onActivity(activity -> {
            openConfirmation(activity);
            activity.expenseListAdapter.filterBy(new Filters(List.of(PETROL), Set.of()));

            confirm(activity);
        });
        scenario.onActivity(activity -> {
            assertEquals(1, activity.expenseListAdapter.getExpenses(true).size());
            activity.clearFilters();
            assertEquals(1, activity.expenseListAdapter.getCount());
            assertTotal(activity, "20.00");
            assertFalse(database.expenseDao().fetchAllExpenses().stream().anyMatch(entity -> entity.id == firstId));
        });
    }

    @Test
    public void cancellingConfirmationLeavesPersistenceAndListUnchanged() {
        final AtomicInteger notifications = new AtomicInteger();
        scenario.onActivity(activity -> {
            observeChanges(activity, notifications);
            openConfirmation(activity);

            activity.deleteConfirmationDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        });
        scenario.onActivity(activity -> {
            assertEquals(2, database.expenseDao().fetchAllExpenses().size());
            assertEquals(0, notifications.get());
            assertEquals(2, activity.expenseListAdapter.getCount());
            assertTotal(activity, "30.00");
        });
    }

    private void assertTotal(final AbstractExpenseListActivity activity, final String expected) {
        final String formattedTotal = ExpenseFormatUtil.amountFormatter().format(new BigDecimal(expected));
        assertEquals(formattedTotal, activity.totalTextView.getText().toString());
    }
}
