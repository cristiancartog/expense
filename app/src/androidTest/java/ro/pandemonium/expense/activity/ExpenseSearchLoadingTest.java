package ro.pandemonium.expense.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;
import static ro.pandemonium.expense.activity.ExpenseActivityTestSupport.awaitExpensesLoaded;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.db.ExpenseDatabase;
import ro.pandemonium.expense.db.ExpenseEntity;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.service.ExpenseSearchService;
import ro.pandemonium.expense.util.ExpenseFormatUtil;

@RunWith(AndroidJUnit4.class)
public class ExpenseSearchLoadingTest {

    private final CountDownLatch queryStarted = new CountDownLatch(1);
    private final CountDownLatch releaseQuery = new CountDownLatch(1);
    private volatile boolean delayQuery;
    private Application application;
    private Application.ActivityLifecycleCallbacks callbacks;
    private ExpenseDatabase database;
    private ActivityScenario<ExpenseSearchResultActivity> scenario;

    @Before
    public void setUp() {
        application = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(application, ExpenseDatabase.class)
                       .allowMainThreadQueries()
                       .setQueryCallback((sql, args) -> {
                           if (sql.contains("WHERE EXPENSE_TYPE IN")) {
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
                           }
                       }, Runnable::run)
                       .build();
        final ExpenseEntity expense = new ExpenseEntity();
        expense.expenseType = FOOD;
        expense.date = LocalDate.of(2000, 1, 1);
        expense.value = new BigDecimal("10.00");
        expense.comment = "search loading test";
        database.expenseDao().insert(expense);
        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(final Activity activity, final Bundle state) {
                if (activity instanceof ExpenseSearchResultActivity search) {
                    // Replace the injected service before the initial search can read installed data.
                    search.searchService = new ExpenseSearchService(database.expenseDao());
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

    private void launchSearch() {
        final Intent intent = new Intent(application, ExpenseSearchResultActivity.class);
        intent.putExtra(INTENT_FILTERS, new Filters(List.of(FOOD), Set.of()));
        scenario = ActivityScenario.launch(intent);
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
    public void delayedSearchKeepsTheScreenResponsiveAndBlocksFiltersAndEditing() throws Exception {
        delayQuery = true;
        launchSearch();
        assertTrue("Search query did not start", queryStarted.await(5, TimeUnit.SECONDS));
        scenario.onActivity(activity -> {
            assertFalse(activity.canUseExpenses());
            assertFalse(activity.expenseList.isEnabled());
            assertFalse(activity.filtersButton.isEnabled());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.expenseListLoadProgress).getVisibility());
            activity.showSearchResultsFiltersDialog(null);
            activity.addEditExpense(0);
            activity.deleteButtonPressed(new View(activity));
            assertFalse(activity.expenseTypeSelectionDialog.isShowing());
            assertFalse(activity.deleteConfirmationDialog.isShowing());
        });

        releaseQuery.countDown();

        awaitExpensesLoaded(scenario);
        scenario.onActivity(this::assertLoaded);
    }

    @Test
    public void failedSearchUsesTheSharedXmlRetryHandlerAndRecovers() {
        database.getOpenHelper().getWritableDatabase().execSQL("ALTER TABLE EXPENSES RENAME TO UNAVAILABLE_EXPENSES");
        launchSearch();
        awaitRetry();
        scenario.onActivity(activity -> {
            assertFalse(activity.canUseExpenses());
            assertFalse(activity.filtersButton.isEnabled());
            assertEquals(0, activity.expenseListAdapter.getCount());
            final TextView status = activity.findViewById(R.id.expenseListLoadStatus);
            assertEquals(activity.getString(R.string.expenseListInitialLoadError), status.getText().toString());
            activity.findViewById(R.id.expenseListRetryLoad).performClick();
        });
        awaitRetry();
        database.getOpenHelper().getWritableDatabase().execSQL("ALTER TABLE UNAVAILABLE_EXPENSES RENAME TO EXPENSES");
        scenario.onActivity(activity -> activity.findViewById(R.id.expenseListRetryLoad).performClick());

        awaitExpensesLoaded(scenario);
        scenario.onActivity(this::assertLoaded);
    }

    private void awaitRetry() {
        final AtomicBoolean retryVisible = new AtomicBoolean();
        final long deadline = SystemClock.uptimeMillis() + 5_000;
        while (!retryVisible.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> retryVisible.set(activity.findViewById(R.id.expenseListRetryLoad).getVisibility() == View.VISIBLE));
            if (!retryVisible.get()) {
                SystemClock.sleep(20);
            }
        }
        assertTrue("Search error did not appear", retryVisible.get());
    }

    private void assertLoaded(final ExpenseSearchResultActivity activity) {
        assertTrue(activity.expenseList.isEnabled());
        assertTrue(activity.filtersButton.isEnabled());
        assertEquals(1, activity.expenseListAdapter.getCount());
        assertEquals(ExpenseFormatUtil.amountFormatter().format(new BigDecimal("10.00")), activity.totalTextView.getText().toString());
        assertEquals(View.GONE, activity.findViewById(R.id.expenseListLoadProgress).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.expenseListLoadStatus).getVisibility());
        assertEquals(View.GONE, activity.findViewById(R.id.expenseListRetryLoad).getVisibility());
    }
}
