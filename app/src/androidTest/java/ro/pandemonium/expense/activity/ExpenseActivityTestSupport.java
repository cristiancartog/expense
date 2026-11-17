package ro.pandemonium.expense.activity;

import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

final class ExpenseActivityTestSupport {

    private ExpenseActivityTestSupport() {
    }

    static void awaitExpensesLoaded(final ActivityScenario<? extends AbstractExpenseListActivity> scenario) {
        final AtomicReference<AbstractExpenseListActivity> activity = new AtomicReference<>();
        scenario.onActivity(activity::set);
        awaitExpensesLoaded(activity.get());
    }

    static void awaitExpensesLoaded(final AbstractExpenseListActivity activity) {
        final AtomicBoolean loaded = new AtomicBoolean();
        final long deadline = SystemClock.uptimeMillis() + 5_000;
        while (!loaded.get() && SystemClock.uptimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> loaded.set(activity.canUseExpenses()));
            if (!loaded.get()) {
                SystemClock.sleep(20);
            }
        }
        assertTrue("Expenses did not load", loaded.get());
    }
}
