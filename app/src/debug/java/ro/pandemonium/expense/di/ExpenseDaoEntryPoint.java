package ro.pandemonium.expense.di;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import ro.pandemonium.expense.db.ExpenseDao;

@EntryPoint
@InstallIn(SingletonComponent.class)
public interface ExpenseDaoEntryPoint {

    ExpenseDao expenseDao();
}
