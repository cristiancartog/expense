package ro.pandemonium.expense.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;

public class ExpenseSearchResultActivity extends AbstractExpenseListActivity
        implements View.OnClickListener,
        AdapterView.OnItemClickListener,
        View.OnLongClickListener {

    public static final int EXPENSE_SEARCH_RESULT_ACTIVITY_ID = 3;

    private final List<Expense> changedExpenses = new ArrayList<>();

    public ExpenseSearchResultActivity() {
    }

    @Override
    int getLayoutId() {
        return R.layout.expense_search_result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        expenseList = (ListView) findViewById(R.id.expansesSearchResultListView);
        expenseList.setAdapter(expenseListAdapter);
        expenseList.setOnItemClickListener(this);

        final Filters filters = (Filters) getIntent().getSerializableExtra(Constants.INTENT_FILTERS);
        List<Expense> expenses = expenseDao.fetchExpenses(filters);

        totalTextView = (TextView) findViewById(R.id.expenseSearchResultTotal);
        filtersButton = (ImageButton) findViewById(R.id.expenseSearchResultFiltersButton);
        filtersButton.setOnLongClickListener(this);

        repopulateExpenseList(expenses);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.sorting_menu, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.expenseSearchResultFiltersButton:
                showFiltersDialog();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        intent.putExtra(Constants.INTENT_CHANGED_EXPENSES, (Serializable) changedExpenses);
        setResult(RESULT_OK, intent);

        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_order_by_date_ascending:
            case R.id.main_menu_order_by_date_descending:
            case R.id.main_menu_order_by_type_ascending:
            case R.id.main_menu_order_by_type_descending:
            case R.id.main_menu_order_by_value_ascending:
            case R.id.main_menu_order_by_value_descending:
                sortExpenses(item.getOrder());
                break;
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        addEditExpense(position);
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.expenseSearchResultFiltersButton:
                clearFilters();
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
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
        Collections.sort(expenses, mapSortingMenuItemToComparator.get(1));
        expenseListAdapter.updateExpenseList(expenses);
        updateTotal();
    }
}
