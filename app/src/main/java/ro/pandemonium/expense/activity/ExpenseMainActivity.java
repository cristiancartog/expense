package ro.pandemonium.expense.activity;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.Constants.INTENT_CHANGED_EXPENSES;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_COUNT_MAP;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import androidx.appcompat.widget.SwitchCompat;

import java.io.Serializable;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.ExpenseHistoryChartActivity;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.service.DatabaseTransferProgress;
import ro.pandemonium.expense.service.ExpenseMainService;
import ro.pandemonium.expense.service.ExpenseMainState;
import ro.pandemonium.expense.service.ExpenseMainUpdate;
import ro.pandemonium.expense.view.ExpenseTypeUi;
import ro.pandemonium.expense.util.TaskRunner;

@AndroidEntryPoint
public class ExpenseMainActivity extends AbstractExpenseListActivity
        implements DatePickerDialog.OnDateSetListener,
            AdapterView.OnItemClickListener,
            CompoundButton.OnCheckedChangeListener {

    @Inject
    ExpenseMainService mainService;

    private final ActivityResultLauncher<Intent> searchResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> processSearchResultActivityResult(result.getResultCode(), result.getData())
    );
    private final ActivityResultLauncher<String[]> backupFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            this::processSelectedBackupFile
    );

    private YearMonth yearMonth;

    private DrawerLayout drawerLayout;
    private Button changeMonthButton;
    private PieChart pieChart;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault());
    // dialogs
    private Filters lastUsedGlobalFilters;
    private TaskRunner expenseLoader;
    private TaskRunner databaseTransferRunner;
    private AlertDialog databaseTransferDialog;
    private ProgressBar databaseTransferProgress;
    private TextView databaseTransferStatus;
    private TextView databaseTransferPercentage;

    public ExpenseMainActivity() {
    }

    @Override
    int layoutId() {
        return R.layout.expense_main_drawer;
    }

    @Override
    int toolbarId() {
        return R.id.main_expense_toolbar;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        expenseLoader = new TaskRunner(this);
        databaseTransferRunner = new TaskRunner(this);

        drawerLayout = findViewById(R.id.main_drawer);

        Toolbar toolbar = findViewById(R.id.main_expense_toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        expenseList = findViewById(R.id.expansesListView);
        expenseList.setAdapter(expenseListAdapter);
        expenseList.setOnItemClickListener(this);

        totalTextView = findViewById(R.id.expenseListTotal);
        changeMonthButton = findViewById(R.id.expenseListChangeMonthButton);
        changeMonthButton.setOnClickListener(this::changeMonth);

        filtersButton = findViewById(R.id.expenseListFiltersButton);
        filtersButton.setOnLongClickListener(this::clearFilters);

        pieChart = findViewById(R.id.expenseListPieChart);
        pieChart.setDescription(null);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.highlightValues(null); // undo all highlights

        SwitchCompat listChartSwitch = findViewById(R.id.expenseListSwitchChartReportButton);
        listChartSwitch.setOnCheckedChangeListener(this);

        yearMonth = YearMonth.now();

        loadExpenses(mainService::initialize);
    }

    private void changeMonth(final View view) {
        new DatePickerDialog(this, this, yearMonth.getYear(), yearMonth.getMonthValue() - 1, 1).show();
    }

    public void listNextMonth(final View view) {
        loadExpenses(mainService::nextMonth);
    }

    public void listPreviousMonth(final View view) {
        loadExpenses(mainService::previousMonth);
    }

    public void addExpense(final View view) {
        final Intent addExpenseIntent = new Intent(this, AddEditExpenseActivity.class);
        addExpenseIntent.putExtra(INTENT_EXPENSE_COUNT_MAP, new HashMap<>(expenseListAdapter.getExpenseTypes()));
        launchAddEditExpense(addExpenseIntent);
    }

    public void filterList(final View view) {
        showFiltersDialog();
    }

    private boolean clearFilters(final View view) {
        super.clearFilters();
        return true;
    }

    public void dbSearchRequested(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        expenseTypeSelectionDialog.show(lastUsedGlobalFilters,
                ExpenseType.orderedExpenseTypes(),
                filters -> {
                    lastUsedGlobalFilters = filters;

                    final Intent searchDatabaseIntent = new Intent(ExpenseMainActivity.this, ExpenseSearchResultActivity.class);
                    searchDatabaseIntent.putExtra(INTENT_FILTERS, filters);
                    searchResultLauncher.launch(searchDatabaseIntent);
                });
    }

    public void specialExpensesRequested(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        Intent intent = new Intent(ExpenseMainActivity.this, ExpenseSearchResultActivity.class);
        intent.putExtra(INTENT_FILTERS, new Filters(Collections.singletonList(ExpenseType.SPECIAL), Collections.emptySet()));
        startActivity(intent);
    }

    public void subscriptionExpensesRequested(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        Intent intent = new Intent(ExpenseMainActivity.this, ExpenseSearchResultActivity.class);
        intent.putExtra(INTENT_FILTERS, new Filters(ExpenseType.subscriptionExpenses(), Collections.emptySet()));
        startActivity(intent);
    }

    public void recurrentExpensesRequested(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        Intent intent = new Intent(ExpenseMainActivity.this, ExpenseSearchResultActivity.class);
        intent.putExtra(INTENT_FILTERS, new Filters(ExpenseType.recurrentExpenses(), Collections.emptySet()));
        startActivity(intent);
    }

    public void yearlyReportRequested(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        Intent yearlyReportIntent = new Intent(ExpenseMainActivity.this, YearlyExpenseReportActivity.class);
        yearlyReportIntent.putExtra(INTENT_YEAR, yearMonth.getYear());

        startActivity(yearlyReportIntent);
    }

    public void expenseHistoryRequested(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        expenseTypeSelectionDialog.show(null,
                Arrays.asList(ExpenseType.values()),
                filters -> {
                    final Intent intent = new Intent(ExpenseMainActivity.this, ExpenseHistoryChartActivity.class);
                    intent.putExtra(INTENT_FILTERS, (Serializable) filters.expenseTypes());
                    startActivity(intent);
                });
    }

    public void dbDumpRequested(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        showDatabaseTransferDialog(R.string.databaseDumpProgressTitle, R.string.databaseDumpPreparing);
        databaseTransferRunner.submit(
                mainService::dumpDatabase,
                this::updateDatabaseTransferDialog,
                this::failDatabaseTransfer,
                () -> finishDatabaseTransfer(R.string.expenseListActivityExportSuccessful, false)
        );
    }

    public void dbRestoreRequested(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        // File providers do not agree on a MIME type for CSV, so do not hide valid backups.
        backupFileLauncher.launch(new String[]{"*/*"});
    }

    @Override
    void processAddEditExpenseResult(final int resultCode, final Intent data) {
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        final Expense expense = requireNonNull(data.getSerializableExtra(INTENT_EXPENSE, Expense.class));
        final ExpenseMainUpdate update = mainService.saveExpense(expense);
        if (update.clearFilters()) {
            clearFilters();
        }
        applyUpdate(update);

        expenseListAdapter.notifyDataSetChanged();

        if (update.operationType() == ExpenseMainUpdate.OperationType.ADD) {
            expenseList.smoothScrollToPosition(expenseListAdapter.getCount());
        }

        updateTotal();
        updatePieData();
    }

    private void processSelectedBackupFile(final Uri backupFile) {
        if (backupFile == null) {
            return;
        }

        showDatabaseTransferDialog(R.string.databaseRestoreProgressTitle, R.string.databaseRestoreReading);
        databaseTransferRunner.submit(
                progressPublisher -> mainService.restoreDatabase(backupFile, progressPublisher),
                this::updateDatabaseTransferDialog,
                this::failDatabaseTransfer,
                () -> finishDatabaseTransfer(R.string.expenseListActivityRestoreSuccessful, true)
        );
    }

    private void showDatabaseTransferDialog(final int titleResource, final int statusResource) {
        final View content = getLayoutInflater().inflate(R.layout.database_transfer_progress, null);
        databaseTransferStatus = content.findViewById(R.id.databaseTransferStatus);
        databaseTransferProgress = content.findViewById(R.id.databaseTransferProgress);
        databaseTransferPercentage = content.findViewById(R.id.databaseTransferPercentage);
        databaseTransferStatus.setText(statusResource);
        databaseTransferProgress.setIndeterminate(true);
        databaseTransferPercentage.setText("");

        databaseTransferDialog = new AlertDialog.Builder(this)
                .setTitle(titleResource)
                .setView(content)
                .setCancelable(false)
                .create();
        databaseTransferDialog.show();
    }

    private void updateDatabaseTransferDialog(final DatabaseTransferProgress progress) {
        if (databaseTransferDialog == null || !databaseTransferDialog.isShowing()) {
            return;
        }

        final int percentage = progress.total() == 0
                ? 100
                : progress.completed() * 100 / progress.total();
        databaseTransferProgress.setIndeterminate(false);
        databaseTransferProgress.setProgress(percentage, true);
        databaseTransferStatus.setText(getString(
                progress.restore() ? R.string.databaseRestoreProgress : R.string.databaseDumpProgress,
                progress.completed(), progress.total()));
        databaseTransferPercentage.setText(getString(R.string.databaseProgressPercentage, percentage));
    }

    private void finishDatabaseTransfer(final int successMessage,
                                        final boolean refreshExpenses) {
        dismissDatabaseTransferDialog();
        Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show();
        if (refreshExpenses) {
            loadExpenses(mainService::reload);
        }
    }

    private void failDatabaseTransfer(final Exception error) {
        dismissDatabaseTransferDialog();
        final String message = error.getMessage() != null ? error.getMessage() : error.toString();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void dismissDatabaseTransferDialog() {
        if (databaseTransferDialog != null) {
            databaseTransferDialog.dismiss();
            databaseTransferDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        dismissDatabaseTransferDialog();
        super.onDestroy();
    }

    @SuppressWarnings("unchecked")
    private void processSearchResultActivityResult(final int resultCode, final Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            final List<Expense> changedExpenses = (List<Expense>) data.getSerializableExtra(INTENT_CHANGED_EXPENSES, ArrayList.class);
            final List<ExpenseMainUpdate> updates = mainService.reconcileChanges(
                    expenseListAdapter.getExpenses(true),
                    changedExpenses != null ? changedExpenses : List.of()
            );
            if (updates.stream().anyMatch(ExpenseMainUpdate::clearFilters)) {
                clearFilters();
            }
            updates.forEach(this::applyUpdate);
            expenseListAdapter.notifyDataSetChanged();
            updateTotal();
            updatePieData();
        }
    }

    private void applyUpdate(final ExpenseMainUpdate update) {
        switch (update.operationType()) {
            case ADD -> expenseListAdapter.addExpense(update.expense());
            case UPDATE -> expenseListAdapter.updateExpense(update.expense());
            case REMOVE -> expenseListAdapter.deleteExpense(update.expense());
            case NONE -> { }
        }
    }

    @Override
    public void onDateSet(final DatePicker datePicker, final int year, final int monthOfYear, final int dayOfMonth) {
        final YearMonth selectedYearMonth = YearMonth.of(year, monthOfYear + 1);
        if (!yearMonth.equals(selectedYearMonth)) {
            loadExpenses(() -> mainService.selectMonth(selectedYearMonth));
        }
    }

    private void loadExpenses(final Supplier<ExpenseMainState> operation) {
        expenseLoader.submit(operation, this::showExpenses);
    }

    private void showExpenses(final ExpenseMainState state) {
        yearMonth = state.yearMonth();
        clearFilters();
        expenseListAdapter.updateExpenseList(state.expenses());
        updateTotal();
        updateMonthButtonLabel();
        expenseList.smoothScrollToPosition(expenseListAdapter.getCount());
        updatePieData();
    }

    @Override
    protected void filtersUpdated() {
        updatePieData();
    }

    private void updatePieData() {
        updateMonthButtonLabel();

        Map<ExpenseType, Double> expenseTypeValues = mainService.totalsByExpenseType(expenseListAdapter.getExpenses(false));

        final List<PieEntry> entries = new ArrayList<>();
        final Resources resources = getResources();
        for (Map.Entry<ExpenseType, Double> entry : expenseTypeValues.entrySet()) {
            String text = resources.getString(ExpenseTypeUi.getTextResource(entry.getKey()));
            entries.add(new PieEntry(entry.getValue().floatValue(), text));
        }

        final PieDataSet pieDataSet = new PieDataSet(entries, null);

        pieDataSet.setSliceSpace(5f);
        pieDataSet.setSelectionShift(15f);

        ArrayList<Integer> colors = new ArrayList<>();
        for (ExpenseType expenseType : ExpenseType.values()) {
            int color = ExpenseTypeUi.getColor(expenseType);
            if (color != Color.WHITE) {
                colors.add(color);
            }
        }

        pieDataSet.setColors(colors);

        PieData data = new PieData(pieDataSet);
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.notifyDataSetChanged();
        pieChart.invalidate();
    }

    private void updateMonthButtonLabel() {
        changeMonthButton.setText(yearMonth.format(dateFormatter));
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        addEditExpense(position);
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        if (buttonView.getId() == R.id.expenseListSwitchChartReportButton) {
            pieChart.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            expenseList.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        }
    }

}
