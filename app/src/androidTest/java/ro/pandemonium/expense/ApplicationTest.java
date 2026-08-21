package ro.pandemonium.expense;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

    @Test
    public void applicationStartsAndInitializesExpenseDao() {
        final Context context = ApplicationProvider.getApplicationContext();

        assertTrue(context instanceof ExpenseApplication);
        assertNotNull(((ExpenseApplication) context).getExpenseDao());
    }
}
