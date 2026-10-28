package ro.pandemonium.expense.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.ExpenseOpenHelper;

@Module
@InstallIn(SingletonComponent.class)
public final class ExpenseModule {

    private ExpenseModule() {
    }

    @Provides
    @Singleton
    static ExpenseDao provideExpenseDao(@ApplicationContext final Context context) {
        // The singleton DAO owns the application-scoped writable database for the process lifetime.
        @SuppressWarnings("resource")
        final ExpenseOpenHelper openHelper = new ExpenseOpenHelper(context);
        return new ExpenseDao(openHelper.getWritableDatabase());
    }
}
