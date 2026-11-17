package ro.pandemonium.expense.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.mikephil.charting.charts.PieChart;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDatabase;
import ro.pandemonium.expense.db.ExpenseEntity;
import ro.pandemonium.expense.service.ExpenseMainService;
import ro.pandemonium.expense.util.ExpenseFormatUtil;

@RunWith(AndroidJUnit4.class)
public class ExpenseMainLoadingTest {

    private final YearMonth currentMonth = YearMonth.now();
    private final AtomicInteger queryCount = new AtomicInteger();
    private final CountDownLatch queryStarted = new CountDownLatch(1);
    private final CountDownLatch releaseQuery = new CountDownLatch(1);
    private volatile boolean delayQuery;
    private Application application;
    private Application.ActivityLifecycleCallbacks callbacks;
    private ExpenseDatabase database;
    private ActivityScenario<ExpenseMainActivity> scenario;

    @Before
    public void setUp() {
        application = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(application, ExpenseDatabase.class)
                       .allowMainThreadQueries()
                       .setQueryCallback((sql, args) -> {
                           if (!sql.contains("STRFTIME('%Y-%m', DATE) =")) {
                               return;
                           }
                           queryCount.incrementAndGet();
                           queryStarted.countDown();
                           if (delayQuery) {
                               try {
                                   if (!releaseQuery.await(10, TimeUnit.SECONDS)) {
                                       throw new IllegalStateException("Timed out waiting to release test query");
                                   }
                               } catch (InterruptedException error) {
                                   Thread.currentThread().interrupt();
                                   throw new IllegalStateException(error);
                               }
                           }
                       }, Runnable::run)
                       .build();
        insert(currentMonth, "10.00");
        insert(currentMonth.plusMonths(1), "20.00");
        insert(currentMonth.minusMonths(1), "30.00");
        insert(currentMonth.minusYears(1), "40.00");

        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(final Activity activity, final Bundle state) {
                if (activity instanceof ExpenseMainActivity main) {
                    // Hilt has injected by this point, but the main activity has not submitted its initial load.
                    main.mainService = new ExpenseMainService(database.expenseDao(), application);
                }
            }

            @Override public void onActivityStarted(final Activity activity) { }
            @Override public void onActivityResumed(final Activity activity) { }
            @Override public void onActivityPaused(final Activity activity) { }
            @Override public void onActivityStopped(final Activity activity) { }
            @Override public void onActivitySaveInstanceState(final Activity activity, final Bundle state) { }
            @Override public void onActivityDestroyed(final Activity activity) { }
        };
        application.registerActivityLifecycleCallbacks(callbacks);
    }

    private void insert(final YearMonth month, final String amount) {
        final ExpenseEntity expense = new ExpenseEntity();
        expense.expenseType = FOOD;
        expense.date = month.atDay(1);
        expense.value = new BigDecimal(amount);
        expense.comment = "loading test";
        database.expenseDao().insert(expense);
    }

    private void setDatabaseAvailable(final boolean available) {
        // Only rename the table in this test's in-memory database to cause a real read failure.
        database.getOpenHelper().getWritableDatabase().execSQL(available
                ? "ALTER TABLE UNAVAILABLE_EXPENSES RENAME TO EXPENSES"
                : "ALTER TABLE EXPENSES RENAME TO UNAVAILABLE_EXPENSES");
    }

    @After
    public void tearDown() {
        releaseQuery.countDown();
        if (scenario != null) {
            scenario.close();
        }
        if (application != null && callbacks != null) {
            application.unregisterActivityLifecycleCallbacks(callbacks);
        }
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void delayedInitialLoadBlocksNavigationAndEditingUntilSuccess() throws Exception {
        delayQuery = true;
        scenario = ActivityScenario.launch(ExpenseMainActivity.class);
        assertTrue("Initial query did not start", queryStarted.await(5, TimeUnit.SECONDS));
        scenario.onActivity(activity -> {
            assertActionsEnabled(activity, false);
            assertEquals(View.VISIBLE, activity.findViewById(R.id.expenseListLoadProgress).getVisibility());
            assertEquals(activity.getString(R.string.expenseListLoading), status(activity));
            activity.findViewById(R.id.expenseListNextMonth).performClick();
            activity.listNextMonth(null);
            activity.listPreviousMonth(null);
            activity.changeMonth(null);
            activity.onDateSet(null, 2000, 0, 1);
            activity.addExpense(null);
            activity.filterList(null);
            activity.onItemClick(null, null, 0, 0);
            activity.deleteButtonPressed(new View(activity));
        });
        assertEquals(1, queryCount.get());

        releaseQuery.countDown();

        await(activity -> activity.findViewById(R.id.addExpenseButton).isEnabled());
        scenario.onActivity(activity -> {
            assertActionsEnabled(activity, true);
            assertMonthAndAmount(activity, currentMonth, "10.00");
            assertLoadStatusHidden(activity);
        });
        assertEquals(1, queryCount.get());
    }

    @Test
    public void failedInitialLoadCanFailAgainAndThenRecoverThroughRetry() {
        setDatabaseAvailable(false);
        scenario = ActivityScenario.launch(ExpenseMainActivity.class);
        awaitRetry();
        scenario.onActivity(activity -> {
            assertActionsEnabled(activity, false);
            assertEquals(activity.getString(R.string.expenseListInitialLoadError), status(activity));
            assertEquals(0, activity.expenseListAdapter.getCount());
            assertEquals(View.GONE, activity.findViewById(R.id.expenseListLoadProgress).getVisibility());
            activity.findViewById(R.id.expenseListRetryLoad).performClick();
        });
        awaitRetry();
        assertEquals(2, queryCount.get());
        setDatabaseAvailable(true);
        scenario.onActivity(activity -> activity.findViewById(R.id.expenseListRetryLoad).performClick());

        await(activity -> activity.findViewById(R.id.addExpenseButton).isEnabled());
        scenario.onActivity(activity -> {
            assertActionsEnabled(activity, true);
            assertMonthAndAmount(activity, currentMonth, "10.00");
            assertLoadStatusHidden(activity);
        });
        assertEquals(3, queryCount.get());
    }

    @Test
    public void failedMonthChangesKeepVisibleDataAndRetryTheRequestedMonth() {
        scenario = ActivityScenario.launch(ExpenseMainActivity.class);
        await(activity -> activity.findViewById(R.id.addExpenseButton).isEnabled());
        verifyFailedMonthChange(
                activity -> activity.listNextMonth(null),
                currentMonth,
                "10.00",
                currentMonth.plusMonths(1),
                "20.00"
        );
        verifyFailedMonthChange(
                activity -> activity.listPreviousMonth(null),
                currentMonth.plusMonths(1),
                "20.00",
                currentMonth,
                "10.00"
        );
        final YearMonth selectedMonth = currentMonth.minusYears(1);
        verifyFailedMonthChange(
                activity -> activity.onDateSet(null, selectedMonth.getYear(), selectedMonth.getMonthValue() - 1, 1),
                currentMonth,
                "10.00",
                selectedMonth,
                "40.00"
        );
    }

    private void verifyFailedMonthChange(final Consumer<ExpenseMainActivity> navigate,
                                         final YearMonth originalMonth,
                                         final String originalAmount,
                                         final YearMonth requestedMonth,
                                         final String requestedAmount) {
        setDatabaseAvailable(false);
        scenario.onActivity(navigate::accept);
        awaitRetry();
        scenario.onActivity(activity -> {
            assertActionsEnabled(activity, true);
            assertMonthAndAmount(activity, originalMonth, originalAmount);
            assertEquals(activity.getString(R.string.expenseListMonthLoadError, monthLabel(originalMonth)), status(activity));
            setDatabaseAvailable(true);
            activity.findViewById(R.id.expenseListRetryLoad).performClick();
            assertActionsEnabled(activity, false);
        });
        await(activity -> activity.findViewById(R.id.addExpenseButton).isEnabled());
        scenario.onActivity(activity -> {
            assertMonthAndAmount(activity, requestedMonth, requestedAmount);
            assertLoadStatusHidden(activity);
        });
    }

    private void awaitRetry() {
        await(activity -> activity.findViewById(R.id.expenseListRetryLoad).getVisibility() == View.VISIBLE);
    }

    private void await(final Predicate<ExpenseMainActivity> condition) {
        final AtomicBoolean complete = new AtomicBoolean();
        final long deadline = SystemClock.uptimeMillis() + 5_000;
        while (!complete.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> complete.set(condition.test(activity)));
            if (!complete.get()) {
                SystemClock.sleep(20);
            }
        }
        assertTrue("Load state did not settle", complete.get());
    }

    private void assertActionsEnabled(final ExpenseMainActivity activity, final boolean enabled) {
        for (int id : new int[]{R.id.expenseListPreviousMonth, R.id.expenseListNextMonth,
                R.id.expenseListChangeMonthButton, R.id.addExpenseButton, R.id.expenseListFiltersButton,
                R.id.expenseListSwitchChartReportButton, R.id.expansesListView}) {
            assertEquals("Unexpected enabled state for " + id, enabled, activity.findViewById(id).isEnabled());
        }
        final DrawerLayout drawer = activity.findViewById(R.id.main_drawer);
        assertEquals(enabled ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                drawer.getDrawerLockMode(activity.findViewById(R.id.main_navigation_view)));
    }

    private void assertMonthAndAmount(final ExpenseMainActivity activity, final YearMonth month, final String amount) {
        final TextView monthButton = activity.findViewById(R.id.expenseListChangeMonthButton);
        assertEquals(monthLabel(month), monthButton.getText().toString());
        assertEquals(1, activity.expenseListAdapter.getCount());
        assertEquals(ExpenseFormatUtil.amountFormatter().format(new BigDecimal(amount)), activity.totalTextView.getText().toString());
        final PieChart chart = activity.findViewById(R.id.expenseListPieChart);
        assertEquals(Float.parseFloat(amount), chart.getData().getYValueSum(), 0.001F);
    }

    private String monthLabel(final YearMonth month) {
        return month.format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault()));
    }

    private String status(final ExpenseMainActivity activity) {
        final TextView status = activity.findViewById(R.id.expenseListLoadStatus);
        assertEquals(View.VISIBLE, status.getVisibility());
        return status.getText().toString();
    }

    private void assertLoadStatusHidden(final ExpenseMainActivity activity) {
        assertEquals(View.GONE, activity.findViewById(R.id.expenseListLoadStatus).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.expenseListLoadProgress).getVisibility());
        assertFalse(activity.findViewById(R.id.expenseListRetryLoad).isShown());
    }
}
