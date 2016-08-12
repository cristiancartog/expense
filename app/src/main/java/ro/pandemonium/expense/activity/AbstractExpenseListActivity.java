package ro.pandemonium.expense.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.PieChartActivity;
import ro.pandemonium.expense.activity.dialog.ExpenseTypeSelectionDialog;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.model.comparator.ExpenseDateComparator;
import ro.pandemonium.expense.model.comparator.ExpenseTypeComparator;
import ro.pandemonium.expense.model.comparator.ExpenseValueComparator;
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.view.adapter.ExpenseListAdapter;

public abstract class AbstractExpenseListActivity extends Activity {

    protected ExpenseDao expenseDao;
    protected ListView expenseList;
    protected ExpenseListAdapter expenseListAdapter = new ExpenseListAdapter();

    protected TextView totalTextView;
    protected NumberFormat numberFormatter = new DecimalFormat(Constants.NUMBER_FORMAT_PATTERN);

    protected Map<Integer, Comparator<Expense>> mapSortingMenuItemToComparator = new HashMap<>();

    // dialogs
    protected ExpenseTypeSelectionDialog expenseTypeSelectionDialog;
    protected Filters lastUsedFilters;
    protected Button filtersButton;

    public AbstractExpenseListActivity() {
        mapSortingMenuItemToComparator.put(1, new ExpenseDateComparator(true));
        mapSortingMenuItemToComparator.put(2, new ExpenseDateComparator(false));
        mapSortingMenuItemToComparator.put(3, new ExpenseTypeComparator(true));
        mapSortingMenuItemToComparator.put(4, new ExpenseTypeComparator(false));
        mapSortingMenuItemToComparator.put(5, new ExpenseValueComparator(true));
        mapSortingMenuItemToComparator.put(6, new ExpenseValueComparator(false));
    }

    abstract int getLayoutId();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getLayoutId());

        expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();
        expenseTypeSelectionDialog = new ExpenseTypeSelectionDialog(this);
    }

    protected void showFiltersDialog() {
        expenseTypeSelectionDialog.show(lastUsedFilters,
                expenseListAdapter.expenseTypeSet(),
                new ExpenseTypeSelectionDialog.Callback() {
                    @Override
                    public void expenseTypesSelected(final Filters filters) {
                        lastUsedFilters = filters;
                        expenseListAdapter.filterBy(filters);
                        filtersButton.setText(R.string.expenseListActivityFiltered);
                        updateTotal();
                    }
                });
    }

    protected void sortExpenses(int order) {
        expenseListAdapter.sortExpenses(mapSortingMenuItemToComparator.get(order));
    }

    protected void addEditExpense(int position) {
        final Expense expense = (Expense) expenseListAdapter.getItem(position);

        final Intent addExpenseIntent = new Intent(this, AddEditExpenseActivity.class);
        addExpenseIntent.putExtra(Constants.INTENT_EXPENSE_COUNT_MAP, expenseListAdapter.getExpenseTypes());
        addExpenseIntent.putExtra(Constants.INTENT_EXPENSE_EXPENSE_TO_EDIT, expense);
        startActivityForResult(addExpenseIntent, AddEditExpenseActivity.ADD_EXPENSE_ACTIVITY_ID);
    }

    protected void clearFilters() {
        if (expenseListAdapter.isFiltered()) {
            expenseListAdapter.clearFilters();
            filtersButton.setText(R.string.expenseListActivityFilterButton);
        }
        updateTotal();
    }

    protected void updateTotal() {
        final double total = ExpenseUtil.computeTotalValue(expenseListAdapter.getExpenses(true));
        final String formattedTotal = numberFormatter.format(total);

        if (expenseListAdapter.isFiltered()) {
            final double filteredTotal = ExpenseUtil.computeTotalValue(expenseListAdapter.getExpenses(false));
            final double percentage = total > 0 ? (filteredTotal / total * 100) : 0;

            final String filteredFormattedTotal = getResources().getString(R.string.expenseListActivityFilteredTotalLabel,
                    numberFormatter.format(filteredTotal), numberFormatter.format(percentage), formattedTotal);
            totalTextView.setText(filteredFormattedTotal);
        } else {
            totalTextView.setText(formattedTotal);
        }
    }

    protected void showPieChart() {
        final Intent pieChartIntent = new Intent(this, PieChartActivity.class);
        pieChartIntent.putExtra(Constants.INTENT_EXPENSE_VALUES_BY_TYPE,
                (Serializable) ExpenseUtil.computeExpenseSumByCategory(expenseListAdapter.getExpenses(false)));
        startActivity(pieChartIntent);
    }

    public void deleteButtonPressed(final View view) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.removeExpenseDialogTitle)
                .setMessage(R.string.removeExpenseDialogMessage)
                .setPositiveButton(R.string.removeExpenseDialogYes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Expense expense = expenseListAdapter.deleteExpenseForTag(view.getTag());
                        expenseDao.removeExpense(expense.getId());
                        updateTotal();
                    }
                })
                .setNegativeButton(R.string.removeExpenseDialogNo, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        expenseTypeSelectionDialog.dismiss();
    }
}
