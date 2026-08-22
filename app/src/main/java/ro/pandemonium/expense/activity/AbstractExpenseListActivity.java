package ro.pandemonium.expense.activity;

import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_COUNT_MAP;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_EXPENSE_TO_EDIT;
import static ro.pandemonium.expense.Constants.NUMBER_FORMAT_PATTERN;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

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
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.util.SystemBarInsets;
import ro.pandemonium.expense.view.adapter.ExpenseListAdapter;

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
        mapSortingMenuItemToComparator.append(1, Comparator.comparing(Expense::getTime));
        mapSortingMenuItemToComparator.append(2, Comparator.comparing(Expense::getExpenseType));
        mapSortingMenuItemToComparator.append(3, Comparator.comparing(Expense::getValue));
    }

    abstract int layoutId();

    abstract int toolbarId();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layoutId());
        SystemBarInsets.applyBottomInset(this);

        Toolbar toolbar = findViewById(toolbarId());
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> NavUtils.navigateUpFromSameTask(this));

        expenseDao = ((ExpenseApplication) getApplication()).getExpenseDao();
        expenseTypeSelectionDialog = new ExpenseTypeSelectionDialog(this);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.sorting_menu, menu);
        menu.findItem(R.id.main_menu_order_by_date)
                .setOnMenuItemClickListener(item -> {
                    sortExpenses(item.getOrder());
                    return true;
                });
        menu.findItem(R.id.main_menu_order_by_type)
                .setOnMenuItemClickListener(item -> {
                    sortExpenses(item.getOrder());
                    return true;
                });
        menu.findItem(R.id.main_menu_order_by_value)
                .setOnMenuItemClickListener(item -> {
                    sortExpenses(item.getOrder());
                    return true;
                });
        return true;
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

    private void sortExpenses(int order) {
        expenseListAdapter.sortExpenses(mapSortingMenuItemToComparator.get(order));
    }

    void filtersUpdated() {
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

            final String filteredFormattedTotal = getResources().getString(R.string.abstractExpenseListActivityFilteredTotalLabel,
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
    protected void onDestroy() {
        super.onDestroy();
        expenseTypeSelectionDialog.dismiss();
    }
}
