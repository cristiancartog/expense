package ro.pandemonium.expense;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.components.SingletonComponent;
import ro.pandemonium.expense.db.ExpenseDao;

@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

    @EntryPoint
    @InstallIn(SingletonComponent.class)
    interface ExpenseDaoEntryPoint {
        ExpenseDao expenseDao();
    }

    @Test
    public void applicationStartsAndInitializesExpenseDao() {
        final Context context = ApplicationProvider.getApplicationContext();

        assertTrue(context instanceof ExpenseApplication);
        assertNotNull(EntryPointAccessors.fromApplication(context, ExpenseDaoEntryPoint.class).expenseDao());
    }
}
