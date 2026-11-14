package ro.pandemonium.expense.activity;

import static java.util.Objects.requireNonNull;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.service.ExpenseSearchService;

@AndroidEntryPoint
public class ExpenseSearchResultActivity extends AbstractExpenseListActivity {

    @Inject
    ExpenseSearchService searchService;

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

        totalTextView = findViewById(R.id.expenseSearchResultTotal);
        filtersButton = findViewById(R.id.expenseSearchResultFiltersButton);
        filtersButton.setOnLongClickListener(this::clearDialogFilters);

        final Filters filters = getIntent().getSerializableExtra(Constants.INTENT_FILTERS, Filters.class);
        showExpenses(searchService.initialize(filters));
    }

    public void showSearchResultsFiltersDialog(final View view) {
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

        final Expense expense = data.getSerializableExtra(Constants.INTENT_EXPENSE, Expense.class);
        requireNonNull(expense);
        searchService.update(expense);
        expenseListAdapter.updateExpense(expense);
        expenseListAdapter.notifyDataSetChanged();
        updateTotal();
        updateActivityResult();
    }

    @Override
    void deleteExpense(final Long expenseId) {
        searchService.delete(expenseId);
        updateActivityResult();
    }

    private void updateActivityResult() {
        final Intent result = new Intent();
        result.putExtra(Constants.INTENT_CHANGED_EXPENSES, new ArrayList<>(searchService.changedExpenses()));
        result.putExtra(Constants.INTENT_DELETED_EXPENSE_IDS, new ArrayList<>(searchService.deletedExpenseIds()));
        setResult(RESULT_OK, result);
    }

    private void showExpenses(final List<Expense> expenses) {
        expenseListAdapter.updateExpenseList(expenses);
        updateTotal();
    }
}
