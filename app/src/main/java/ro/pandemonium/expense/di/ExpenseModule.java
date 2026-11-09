package ro.pandemonium.expense.di;

import android.content.Context;

import androidx.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.db.ExpenseDatabase;

@Module
@InstallIn(SingletonComponent.class)
public final class ExpenseModule {

    private ExpenseModule() {
    }

    @Provides
    @Singleton
    static ExpenseDatabase provideExpenseDatabase(@ApplicationContext final Context context) {
        return Room.databaseBuilder(context, ExpenseDatabase.class, ExpenseDatabase.NAME)
                   .allowMainThreadQueries()
                   .build();
    }

    @Provides
    @Singleton
    static ExpenseDao provideExpenseDao(final ExpenseDatabase database) {
        return database.expenseDao();
    }
}
