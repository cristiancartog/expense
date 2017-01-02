package ro.pandemonium.expense.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;

public class SpecialExpensesActivity extends AbstractExpenseListActivity
        implements View.OnClickListener,
        AdapterView.OnItemClickListener {

    public SpecialExpensesActivity() {
    }

    @Override
    int getLayoutId() {
        return R.layout.special_expenses_list;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        expenseList = (ListView) findViewById(R.id.specialExpensesListView);
        expenseList.setAdapter(expenseListAdapter);
        expenseList.setOnItemClickListener(this);

        List<Expense> expenses = expenseDao.fetchSpecialExpenses();

        totalTextView = (TextView) findViewById(R.id.specialExpensesActivityTotalLabel);

        repopulateExpenseList(expenses);
    }

    @Override
    public void onClick(final View view) {
        if (view.getId() == R.id.specialExpensesAddExpenseButton) {
            final Intent addExpenseIntent = new Intent(this, AddEditExpenseActivity.class);
            addExpenseIntent.putExtra(Constants.INTENT_EXPENSE_COUNT_MAP, new HashMap<>(expenseListAdapter.getExpenseTypes()));
            startActivityForResult(addExpenseIntent, AddEditExpenseActivity.ADD_EXPENSE_ACTIVITY_ID);
        }
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
        addEditExpense(position);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == AddEditExpenseActivity.ADD_EXPENSE_ACTIVITY_ID
                && resultCode == RESULT_OK) {
            final Expense expense = (Expense) data.getSerializableExtra(Constants.INTENT_EXPENSE);
            Log.i(Constants.APPLICATION_NAME, expense.toString());

            if (expense.getId() == null) { // new expense
                expenseDao.persistExpense(expense);
                expenseListAdapter.addExpense(expense);
            } else { // updated expense
                expenseDao.updateExpense(expense);
                expenseListAdapter.updateExpense(expense);
            }

            expenseListAdapter.notifyDataSetChanged();
            expenseList.smoothScrollToPosition(expenseListAdapter.getCount());

            updateTotal();
        }
    }

    private void repopulateExpenseList(final List<Expense> expenses) {
        Collections.sort(expenses, mapSortingMenuItemToComparator.get(2));
        expenseListAdapter.updateExpenseList(expenses);
        updateTotal();
    }
}
