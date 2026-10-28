package ro.pandemonium.expense.activity;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.Constants.APPLICATION_NAME;
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
import android.util.Log;
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

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.ExpenseHistoryChartActivity;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.util.FileUtil;
import ro.pandemonium.expense.util.TaskRunner;

public class ExpenseMainActivity extends AbstractExpenseListActivity
        implements DatePickerDialog.OnDateSetListener,
            AdapterView.OnItemClickListener,
            CompoundButton.OnCheckedChangeListener {

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

        repopulateExpenseList(yearMonth);
    }

    private void changeMonth(final View view) {
        new DatePickerDialog(this, this, yearMonth.getYear(), yearMonth.getMonthValue() - 1, 1).show();
    }

    public void listNextMonth(final View view) {
        yearMonth = yearMonth.plusMonths(1);
        repopulateExpenseList(yearMonth);
    }

    public void listPreviousMonth(final View view) {
        yearMonth = yearMonth.minusMonths(1);
        repopulateExpenseList(yearMonth);
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
        databaseTransferRunner.submitWithProgress(
                this::dumpDatabase,
                this::updateDatabaseTransferDialog,
                () -> finishDatabaseTransfer(R.string.expenseListActivityExportSuccessful, false),
                this::failDatabaseTransfer);
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
        Log.i(APPLICATION_NAME, expense.toString());

        final Long expenseId = expense.getId();
        boolean newExpenseAddedThisMonth = false;

        if (expenseId == null) { // add new expense
            expenseDao.persistExpense(expense);

            if (isInSameMonth(expense.getDate())) {
                clearFilters();

                if (expense.getExpenseType() != ExpenseType.SPECIAL) {
                    expenseListAdapter.addExpense(expense);
                    newExpenseAddedThisMonth = true;
                }
            }
        } else { // update existing expense
            expenseDao.updateExpense(expense);
            if (!isInSameMonth(expense.getDate())) {
                expenseListAdapter.deleteExpense(expense);
            } else {
                expenseListAdapter.updateExpense(expense);
            }
        }

        expenseListAdapter.notifyDataSetChanged();

        if (newExpenseAddedThisMonth) {
            expenseList.smoothScrollToPosition(expenseListAdapter.getCount());
        }

        updateTotal();
    }

    private void processSelectedBackupFile(final Uri backupFile) {
        if (backupFile == null) {
            return;
        }

        showDatabaseTransferDialog(R.string.databaseRestoreProgressTitle, R.string.databaseRestoreReading);
        databaseTransferRunner.submitWithProgress(
                progressPublisher -> restoreDatabase(backupFile, progressPublisher),
                this::updateDatabaseTransferDialog,
                () -> finishDatabaseTransfer(R.string.expenseListActivityRestoreSuccessful, true),
                this::failDatabaseTransfer);
    }

    private void dumpDatabase(final Consumer<DatabaseTransferProgress> progressPublisher) throws IOException {
        final List<Expense> expenses = expenseDao.fetchExpenses((YearMonth) null);
        final ProgressPublisher throttledProgress = new ProgressPublisher(expenses.size(), false, progressPublisher);
        throttledProgress.publish(0, true);
        FileUtil.exportExpenses(this, expenses, count -> throttledProgress.publish(count, false));
        throttledProgress.publish(expenses.size(), true);
    }

    private void restoreDatabase(final Uri backupFile,
                                 final Consumer<DatabaseTransferProgress> progressPublisher) throws IOException {
        final List<Expense> expenses = FileUtil.importExpenses(this, backupFile);
        final ProgressPublisher throttledProgress = new ProgressPublisher(expenses.size(), true, progressPublisher);
        throttledProgress.publish(0, true);
        expenseDao.restoreExpenses(expenses, count -> throttledProgress.publish(count, false));
        throttledProgress.publish(expenses.size(), true);
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
            repopulateExpenseList(yearMonth);
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

    private record DatabaseTransferProgress(int completed, int total, boolean restore) { }

    private static final class ProgressPublisher {
        private static final long UPDATE_INTERVAL_MILLIS = 1_000L;

        private final int total;
        private final boolean restore;
        private final Consumer<DatabaseTransferProgress> publisher;
        private long lastPublishedAt;

        private ProgressPublisher(final int total,
                                  final boolean restore,
                                  final Consumer<DatabaseTransferProgress> publisher) {
            this.total = total;
            this.restore = restore;
            this.publisher = publisher;
        }

        private void publish(final int completed, final boolean force) {
            final long now = System.currentTimeMillis();
            if (force || now - lastPublishedAt >= UPDATE_INTERVAL_MILLIS) {
                lastPublishedAt = now;
                publisher.accept(new DatabaseTransferProgress(completed, total, restore));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processSearchResultActivityResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            List<Expense> changedExpenses = (List<Expense>) data.getSerializableExtra(INTENT_CHANGED_EXPENSES, ArrayList.class);
            expenseListAdapter.updateExpenses(changedExpenses != null ? changedExpenses : new ArrayList<>());
            expenseListAdapter.notifyDataSetChanged();
        }
    }

    private boolean isInSameMonth(final LocalDate date) {
        return YearMonth.from(date).equals(yearMonth);
    }

    @Override
    public void onDateSet(final DatePicker datePicker, final int year, final int monthOfYear, final int dayOfMonth) {
        final YearMonth selectedYearMonth = YearMonth.of(year, monthOfYear + 1);
        if (!yearMonth.equals(selectedYearMonth)) {
            yearMonth = selectedYearMonth;
            repopulateExpenseList(yearMonth);
        }
    }

    private void repopulateExpenseList(final YearMonth yearMonth) {
        expenseLoader.submit(
                () -> expenseDao.fetchExpenses(yearMonth),
                this::repopulateExpenseList);
    }

    private void repopulateExpenseList(final List<Expense> expenses) {
        clearFilters();
        expenses.sort(mapSortingMenuItemToComparator.get(1));
        expenseListAdapter.updateExpenseList(expenses);
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

        Map<ExpenseType, Double> expenseTypeValues = ExpenseUtil.computeExpenseSumByCategory(expenseListAdapter.getExpenses(false));

        final List<PieEntry> entries = new ArrayList<>();
        final Resources resources = getResources();
        for (Map.Entry<ExpenseType, Double> entry : expenseTypeValues.entrySet()) {
            String text = resources.getString(entry.getKey().getTextResource());
            entries.add(new PieEntry(entry.getValue().floatValue(), text));
        }

        final PieDataSet pieDataSet = new PieDataSet(entries, null);

        pieDataSet.setSliceSpace(5f);
        pieDataSet.setSelectionShift(15f);

        ArrayList<Integer> colors = new ArrayList<>();
        for (ExpenseType expenseType : ExpenseType.values()) {
            int color = expenseType.getColor();
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
