package ro.pandemonium.expense.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;

import ro.pandemonium.expense.ExpenseApplication;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.dialog.ExpenseTypeSelectionDialog;
import ro.pandemonium.expense.db.ExpenseDao;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.model.comparator.ExpenseDateComparator;
import ro.pandemonium.expense.model.comparator.ExpenseTypeComparator;
import ro.pandemonium.expense.model.comparator.ExpenseValueComparator;
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.view.adapter.ExpenseListAdapter;

import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_COUNT_MAP;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_EXPENSE_TO_EDIT;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;

public abstract class AbstractExpenseListActivity extends AppCompatActivity {

    ExpenseDao expenseDao;
    ListView expenseList;
    final ExpenseListAdapter expenseListAdapter = new ExpenseListAdapter();

    TextView totalTextView;
    private final NumberFormat numberFormatter = new DecimalFormat(NUMBER_FORMAT_PATTERN);

    final SparseArray<Comparator<Expense>> mapSortingMenuItemToComparator = new SparseArray<>();

    // dialogs
    ExpenseTypeSelectionDialog expenseTypeSelectionDialog;
    private Filters lastUsedFilters;
    ImageButton filtersButton;

    AbstractExpenseListActivity() {
        mapSortingMenuItemToComparator.append(1, new ExpenseDateComparator());
        mapSortingMenuItemToComparator.append(2, new ExpenseTypeComparator());
        mapSortingMenuItemToComparator.append(3, new ExpenseValueComparator());
    }

    abstract int layoutId();

    abstract int toolbarId();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layoutId());

        Toolbar toolbar = (Toolbar) findViewById(toolbarId());
        setSupportActionBar(toolbar);

        expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();
        expenseTypeSelectionDialog = new ExpenseTypeSelectionDialog(this);
    }

    void showFiltersDialog() {
        expenseTypeSelectionDialog.show(lastUsedFilters,
                expenseListAdapter.expenseTypeSet(),
                filters -> {
                    lastUsedFilters = filters;
                    expenseListAdapter.filterBy(filters);
                    filtersButton.setImageDrawable(getResources().getDrawable(R.mipmap.ic_filtered));
                    updateTotal();
                    filtersUpdated();
                });
    }

    void sortExpenses(int order) {
        expenseListAdapter.sortExpenses(mapSortingMenuItemToComparator.get(order));
    }

    protected void filtersUpdated() {
    }

    void addEditExpense(int position) {
        final Expense expense = (Expense) expenseListAdapter.getItem(position);

        final Intent addExpenseIntent = new Intent(this, AddEditExpenseActivity.class);
        addExpenseIntent.putExtra(INTENT_EXPENSE_COUNT_MAP, new HashMap<>(expenseListAdapter.getExpenseTypes()));
        addExpenseIntent.putExtra(INTENT_EXPENSE_EXPENSE_TO_EDIT, expense);
        startActivityForResult(addExpenseIntent, AddEditExpenseActivity.ADD_EXPENSE_ACTIVITY_ID);
    }

    void clearFilters() {
        if (expenseListAdapter.isFiltered()) {
            expenseListAdapter.clearFilters();
            filtersButton.setImageDrawable(getResources().getDrawable(R.mipmap.ic_filter));
        }
        updateTotal();
        filtersUpdated();
    }

    void updateTotal() {
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

    public void deleteButtonPressed(final View view) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.removeExpenseDialogTitle)
                .setMessage(R.string.removeExpenseDialogMessage)
                .setPositiveButton(R.string.removeExpenseDialogYes, (dialog, which) -> {
                    final Expense expense = expenseListAdapter.deleteExpenseForTag(view.getTag());
                    expenseDao.removeExpense(expense.getId());
                    updateTotal();
                })
                .setNegativeButton(R.string.removeExpenseDialogNo, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_menu_order_by_date:
            case R.id.main_menu_order_by_type:
            case R.id.main_menu_order_by_value:
                sortExpenses(item.getOrder());
                break;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                break;
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        expenseTypeSelectionDialog.dismiss();
    }
}
