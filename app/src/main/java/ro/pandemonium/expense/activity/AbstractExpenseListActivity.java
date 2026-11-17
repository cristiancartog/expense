package ro.pandemonium.expense.activity;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_COUNT_MAP;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_EXPENSE_TO_EDIT;
import static ro.pandemonium.expense.model.Filters.EMPTY_FILTERS;
import static ro.pandemonium.expense.util.ExpenseMathUtil.percentage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.dialog.ExpenseDeletionDialog;
import ro.pandemonium.expense.activity.dialog.ExpenseTypeSelectionDialog;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.util.ExpenseFormatUtil;
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.util.SystemBarInsets;
import ro.pandemonium.expense.util.TaskRunner;
import ro.pandemonium.expense.view.adapter.ExpenseListAdapter;

public abstract class AbstractExpenseListActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> addEditExpenseLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> processAddEditExpenseResult(result.getResultCode(), result.getData()));

    ListView expenseList;
    final ExpenseListAdapter expenseListAdapter = new ExpenseListAdapter();

    TextView totalTextView;
    private final NumberFormat numberFormatter = ExpenseFormatUtil.amountFormatter();

    final SparseArray<Comparator<Expense>> mapSortingMenuItemToComparator = new SparseArray<>();

    // dialogs
    ExpenseTypeSelectionDialog expenseTypeSelectionDialog;
    ExpenseDeletionDialog deleteConfirmationDialog;
    private Filters lastUsedFilters = EMPTY_FILTERS;
    ImageButton filtersButton;

    private TaskRunner expenseLoader;
    private TextView loadStatus;
    private ProgressBar loadProgress;
    private Button retryLoadButton;
    private boolean loading;
    private boolean initialized;
    private Runnable pendingLoad;

    AbstractExpenseListActivity() {
        mapSortingMenuItemToComparator.append(1, Comparator.comparing(Expense::getDate));
        mapSortingMenuItemToComparator.append(2, Comparator.comparingInt(expense -> expense.getExpenseType().getOrder()));
        mapSortingMenuItemToComparator.append(3, Comparator.comparing(Expense::getValue));
    }

    abstract int layoutId();

    abstract int toolbarId();

    abstract void processAddEditExpenseResult(int resultCode, Intent data);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(layoutId());
        SystemBarInsets.keepContentInsideSystemBars(this);

        Toolbar toolbar = findViewById(toolbarId());
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> NavUtils.navigateUpFromSameTask(this));

        expenseTypeSelectionDialog = new ExpenseTypeSelectionDialog(this);
        deleteConfirmationDialog = new ExpenseDeletionDialog(this);
        expenseLoader = new TaskRunner(this);
        loadStatus = findViewById(R.id.expenseListLoadStatus);
        loadProgress = findViewById(R.id.expenseListLoadProgress);
        retryLoadButton = findViewById(R.id.expenseListRetryLoad);
    }

    public final void retryExpenseLoad(final View view) {
        if (!loading && pendingLoad != null) {
            pendingLoad.run();
        }
    }

    final <T> void loadExpenses(final Supplier<T> operation, final Consumer<T> resultConsumer) {
        if (loading) {
            return;
        }

        pendingLoad = () -> loadExpenses(operation, resultConsumer);
        loading = true;
        loadStatus.setText(R.string.expenseListLoading);
        loadStatus.setVisibility(VISIBLE);
        loadProgress.setVisibility(VISIBLE);
        retryLoadButton.setVisibility(GONE);
        updateExpenseActions(canUseExpenses());
        expenseLoader.submit(
                operation,
                result -> {
                    resultConsumer.accept(result);
                    finishExpenseLoad();
                },
                this::failExpenseLoad
        );
    }

    final boolean canUseExpenses() {
        return initialized && !loading;
    }

    void updateExpenseActions(final boolean enabled) {
        expenseList.setEnabled(enabled);
        filtersButton.setEnabled(enabled);
    }

    private void finishExpenseLoad() {
        initialized = true;
        loading = false;
        pendingLoad = null;
        loadStatus.setVisibility(GONE);
        loadProgress.setVisibility(GONE);
        retryLoadButton.setVisibility(GONE);
        updateExpenseActions(canUseExpenses());
    }

    private void failExpenseLoad(final Exception error) {
        Log.e(getClass().getSimpleName(), "Could not load expenses", error);
        loading = false;
        loadProgress.setVisibility(GONE);
        loadStatus.setText(expenseLoadErrorMessage(initialized));
        retryLoadButton.setVisibility(VISIBLE);
        updateExpenseActions(canUseExpenses());
    }

    String expenseLoadErrorMessage(final boolean initialized) {
        return getString(initialized ? R.string.expenseListReloadError : R.string.expenseListInitialLoadError);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.sorting_menu, menu);
        return true;
    }

    void showFiltersDialog() {
        if (!canUseExpenses()) {
            return;
        }

        expenseTypeSelectionDialog.show(lastUsedFilters,
                expenseListAdapter.expenseTypeSet(),
                filters -> {
                    lastUsedFilters = filters;
                    expenseListAdapter.filterBy(filters);
                    filtersButton.setImageResource(R.mipmap.ic_filtered);
                    renderExpenses();
                });
    }

    public void sortExpenses(final MenuItem item) {
        expenseListAdapter.sortExpenses(mapSortingMenuItemToComparator.get(item.getOrder()));
    }

    void renderExpenses() {
        expenseListAdapter.notifyDataSetChanged();
        updateTotal();
    }

    void addEditExpense(int position) {
        if (!canUseExpenses()) {
            return;
        }

        final Expense expense = (Expense) expenseListAdapter.getItem(position);

        final Intent addExpenseIntent = new Intent(this, AddEditExpenseActivity.class);
        addExpenseIntent.putExtra(INTENT_EXPENSE_COUNT_MAP, new HashMap<>(expenseListAdapter.getExpenseTypes()));
        addExpenseIntent.putExtra(INTENT_EXPENSE_EXPENSE_TO_EDIT, expense);
        launchAddEditExpense(addExpenseIntent);
    }

    final void launchAddEditExpense(final Intent intent) {
        if (!canUseExpenses()) {
            return;
        }

        addEditExpenseLauncher.launch(intent);
    }

    final boolean clearFilters(final View view) {
        if (canUseExpenses()) {
            clearFilters();
        }
        return true;
    }

    void clearFilters() {
        if (expenseListAdapter.isFiltered()) {
            expenseListAdapter.clearFilters();
            filtersButton.setImageResource(R.mipmap.ic_filter);
        }
        renderExpenses();
    }

    void updateTotal() {
        final BigDecimal total = ExpenseUtil.computeTotalValue(expenseListAdapter.getExpenses(true));
        final String formattedTotal = numberFormatter.format(total);

        if (expenseListAdapter.isFiltered()) {
            final BigDecimal filteredTotal = ExpenseUtil.computeTotalValue(expenseListAdapter.getExpenses(false));
            final BigDecimal percentage = percentage(filteredTotal, total);

            final String filteredFormattedTotal = getResources().getString(R.string.abstractExpenseListActivityFilteredTotalLabel,
                    numberFormatter.format(filteredTotal), numberFormatter.format(percentage), formattedTotal);
            totalTextView.setText(filteredFormattedTotal);
        } else {
            totalTextView.setText(formattedTotal);
        }
    }

    public void deleteButtonPressed(final View view) {
        if (!canUseExpenses()) {
            return;
        }

        final Long expenseId = (Long) view.getTag();
        deleteConfirmationDialog.show(() -> confirmExpenseDeletion(expenseId));
    }

    private void confirmExpenseDeletion(final Long expenseId) {
        if (!canUseExpenses()) {
            return;
        }

        try {
            deleteExpense(expenseId);
        } catch (RuntimeException error) {
            Toast.makeText(this, R.string.removeExpenseFailed, Toast.LENGTH_LONG).show();
            return;
        }

        expenseListAdapter.deleteExpense(expenseId);
        renderExpenses();
    }

    abstract void deleteExpense(Long expenseId);

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deleteConfirmationDialog.dismiss();
        expenseTypeSelectionDialog.dismiss();
    }
}
