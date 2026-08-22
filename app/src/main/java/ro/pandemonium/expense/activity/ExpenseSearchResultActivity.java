package ro.pandemonium.expense.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import androidx.appcompat.app.ActionBar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;

public class ExpenseSearchResultActivity extends AbstractExpenseListActivity {

    public static final int EXPENSE_SEARCH_RESULT_ACTIVITY_ID = 3;

    private final List<Expense> changedExpenses = new ArrayList<>();

    public ExpenseSearchResultActivity() {
    }

    @Override
    int layoutId() {
        return R.layout.expense_search_result_appbar;
    }

    @Override
    int toolbarId() {
        return R.id.search_result_toolbar;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }

        expenseList = findViewById(R.id.expansesSearchResultListView);
        expenseList.setAdapter(expenseListAdapter);
        expenseList.setOnItemClickListener(this::expenseListItemClicked);

        final Filters filters = (Filters) getIntent().getSerializableExtra(Constants.INTENT_FILTERS);
        List<Expense> expenses = expenseDao.fetchExpenses(filters);

        totalTextView = findViewById(R.id.expenseSearchResultTotal);
        filtersButton = findViewById(R.id.expenseSearchResultFiltersButton);
        filtersButton.setOnClickListener(this::showSearchResultsFiltersDialog);
        filtersButton.setOnLongClickListener(this::clearDialogFilters);

        repopulateExpenseList(expenses);
    }

    private void showSearchResultsFiltersDialog(final View view) {
        showFiltersDialog();
    }

    private boolean clearDialogFilters(final View view) {
        clearFilters();
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        intent.putExtra(Constants.INTENT_CHANGED_EXPENSES, (Serializable) changedExpenses);
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    private void expenseListItemClicked(AdapterView<?> parent, View view, int position, long id) {
        addEditExpense(position);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AddEditExpenseActivity.ADD_EXPENSE_ACTIVITY_ID
                && resultCode == RESULT_OK) {
            final Expense expense = (Expense) data.getSerializableExtra(Constants.INTENT_EXPENSE);
            Log.i(Constants.APPLICATION_NAME, expense.toString());

            expenseDao.updateExpense(expense);
            expenseListAdapter.updateExpense(expense);
            expenseListAdapter.notifyDataSetChanged();

            updateTotal();

            changedExpenses.add(expense);
        }
    }

    private void repopulateExpenseList(final List<Expense> expenses) {
        expenses.sort(mapSortingMenuItemToComparator.get(1));
        expenseListAdapter.updateExpenseList(expenses);
        updateTotal();
    }
}
