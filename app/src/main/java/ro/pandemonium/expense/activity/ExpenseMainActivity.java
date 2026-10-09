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
import android.widget.Switch;
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

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.ExpenseHistoryChartActivity;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.model.MonthWrapper;
import ro.pandemonium.expense.task.ExpenseLoaderTask;
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.util.FileUtil;
import ro.pandemonium.expense.view.ExpenseTypeColor;

public class ExpenseMainActivity extends AbstractExpenseListActivity
        implements DatePickerDialog.OnDateSetListener,
        AdapterView.OnItemClickListener,
        CompoundButton.OnCheckedChangeListener {

    private final ActivityResultLauncher<Intent> searchResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> processSearchResultActivityResult(result.getResultCode(), result.getData()));
    private final ActivityResultLauncher<String[]> backupFileLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::processSelectedBackupFile);

    private int year;
    private Month month;

    private DrawerLayout drawerLayout;
    private Button changeMonthButton;
    private PieChart pieChart;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault());
    // dialogs
    private Filters lastUsedGlobalFilters;

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

        Switch listChartSwitch = findViewById(R.id.expenseListSwitchChartReportButton);
        listChartSwitch.setOnCheckedChangeListener(this);

        final LocalDate now = LocalDate.now();
        year = now.getYear();
        month = now.getMonth();

        repopulateExpenseList(year, month);
    }

    private void changeMonth(final View view) {
        new DatePickerDialog(this, this, year, month.ordinal(), 1).show();
    }

    public void listNextMonth(final View view) {
        month = month.plus(1);

        if (month == Month.JANUARY) {
            year++;
        }

        repopulateExpenseList(year, month);
    }

    public void listPreviousMonth(final View view) {
        month = month.minus(1);

        if (month == Month.DECEMBER) {
            year--;
        }

        repopulateExpenseList(year, month);
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
        yearlyReportIntent.putExtra(INTENT_YEAR, year);

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
        try {
            final List<Expense> allExpenses = expenseDao.fetchExpenses(null, null);
            FileUtil.exportExpenses(this, allExpenses);
            Toast.makeText(this, R.string.expenseListActivityExportSuccessful, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

        try {
            final List<Expense> expenses = FileUtil.importExpenses(this, backupFile);
            expenseDao.restoreExpenses(expenses);
            repopulateExpenseList(year, month);
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
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
        return date.getYear() == year && date.getMonth() == month;
    }

    @Override
    public void onDateSet(final DatePicker datePicker, final int year, final int monthOfYear, final int dayOfMonth) {
        Month month = Month.of(monthOfYear + 1); // +1 because calendar component uses old Calendar style indexing (0-11)
        if (this.year != year || this.month != month) {
            this.month = month;
            this.year = year;
            repopulateExpenseList(year, month);
        }
    }

    private void repopulateExpenseList(final int year, final Month month) {
        new ExpenseLoaderTask(expenseDao, expenses -> {
            clearFilters();
            expenses.sort(mapSortingMenuItemToComparator.get(1));
            expenseListAdapter.updateExpenseList(expenses);
            updateTotal();
            updateMonthButtonLabel();
            expenseList.smoothScrollToPosition(expenseListAdapter.getCount());

            updatePieData();
        }).execute(new MonthWrapper(year, month));
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
            int color = ExpenseTypeColor.color(expenseType);
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
        final LocalDate date = LocalDate.of(year, month, 1);
        changeMonthButton.setText(date.format(dateFormatter));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        expenseDao.closeDatabase();
    }
}
