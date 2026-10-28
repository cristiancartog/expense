package ro.pandemonium.expense.activity;

import static java.util.Objects.requireNonNull;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;

@AndroidEntryPoint
public class ExpenseSearchResultActivity extends AbstractExpenseListActivity {

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

        final Filters filters = getIntent().getSerializableExtra(Constants.INTENT_FILTERS, Filters.class);
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

    private void expenseListItemClicked(AdapterView<?> parent, View view, int position, long id) {
        addEditExpense(position);
    }

    @Override
    void processAddEditExpenseResult(final int resultCode, final Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        final Expense expense = requireNonNull(data.getSerializableExtra(Constants.INTENT_EXPENSE, Expense.class));
        Log.i(Constants.APPLICATION_NAME, expense.toString());

        expenseDao.updateExpense(expense);
        expenseListAdapter.updateExpense(expense);
        expenseListAdapter.notifyDataSetChanged();

        updateTotal();

        changedExpenses.add(expense);

        final Intent result = new Intent();
        result.putExtra(Constants.INTENT_CHANGED_EXPENSES, new ArrayList<>(changedExpenses));
        setResult(RESULT_OK, result);
    }

    private void repopulateExpenseList(final List<Expense> expenses) {
        expenses.sort(mapSortingMenuItemToComparator.get(1));
        expenseListAdapter.updateExpenseList(expenses);
        updateTotal();
    }
}
