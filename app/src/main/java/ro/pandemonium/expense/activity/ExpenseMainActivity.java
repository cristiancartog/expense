package ro.pandemonium.expense.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.ExpenseHistoryChartActivity;
import ro.pandemonium.expense.activity.dialog.ImportFileSelectionDialog;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.model.MonthWrapper;
import ro.pandemonium.expense.task.ExpenseLoaderTask;
import ro.pandemonium.expense.util.ExpenseUtil;
import ro.pandemonium.expense.util.FileUtil;
import ro.pandemonium.expense.view.ExpenseTypeColor;

import static ro.pandemonium.expense.Constants.APPLICATION_NAME;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.Constants.INTENT_CHANGED_EXPENSES;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_COUNT_MAP;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;
import static ro.pandemonium.expense.Constants.INTENT_YEAR;

public class ExpenseMainActivity extends AbstractExpenseListActivity
        implements View.OnClickListener,
        DatePickerDialog.OnDateSetListener,
        AdapterView.OnItemClickListener,
        View.OnLongClickListener,
        CompoundButton.OnCheckedChangeListener,
        NavigationView.OnNavigationItemSelectedListener {

    private final Calendar calendar = Calendar.getInstance();
    private int year;
    private int month;

    private DrawerLayout drawerLayout;
    private Button changeMonthButton;
    private PieChart pieChart;
    private final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault());

    // dialogs
    private ImportFileSelectionDialog importFileSelectionDialog;
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

        drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_expense_toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.main_navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        expenseList = (ListView) findViewById(R.id.expansesListView);
        expenseList.setAdapter(expenseListAdapter);
        expenseList.setOnItemClickListener(this);

        totalTextView = (TextView) findViewById(R.id.expenseListTotal);
        changeMonthButton = (Button) findViewById(R.id.expenseListChangeMonthButton);
        filtersButton = (ImageButton) findViewById(R.id.expenseListFiltersButton);
        filtersButton.setOnLongClickListener(this);

        pieChart = (PieChart) findViewById(R.id.expenseListPieChart);
        pieChart.setDescription(null);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.highlightValues(null); // undo all highlights

        Switch listChartSwitch = (Switch) findViewById(R.id.expenseListSwitchChartReportButton);
        listChartSwitch.setOnCheckedChangeListener(this);

        importFileSelectionDialog = new ImportFileSelectionDialog(this);

        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH) + 1;

        repopulateExpenseList(year, month);
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.expenseListChangeMonthButton:
                new DatePickerDialog(this, this, year, month - 1, 1).show();
                break;

            case R.id.addExpenseButton:
                final Intent addExpenseIntent = new Intent(this, AddEditExpenseActivity.class);
                addExpenseIntent.putExtra(INTENT_EXPENSE_COUNT_MAP, new HashMap<>(expenseListAdapter.getExpenseTypes()));
                startActivityForResult(addExpenseIntent, AddEditExpenseActivity.ADD_EXPENSE_ACTIVITY_ID);
                break;

            case R.id.expenseListFiltersButton:
                showFiltersDialog();
                break;

            case R.id.expenseListPreviousMonth:
                month--;
                if (month < 1) {
                    month = 12;
                    year--;
                }

                repopulateExpenseList(year, month);
                break;

            case R.id.expenseListNextMonth:
                month++;
                if (month > 12) {
                    month = 1;
                    year++;
                }

                repopulateExpenseList(year, month);
                break;
        }
    }

    @Override
    public boolean onLongClick(final View view) {
        switch (view.getId()) {
            case R.id.expenseListFiltersButton:
                clearFilters();
                break;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.sorting_menu, menu);
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        drawerLayout.closeDrawer(Gravity.START);
        switch (item.getItemId()) {
            case R.id.navigation_search_db:
                dbSearchRequested();
                break;
            case R.id.navigation_special_expenses:
                Intent intent = new Intent(ExpenseMainActivity.this, ExpenseSearchResultActivity.class);
                intent.putExtra(INTENT_FILTERS, new Filters(Collections.singletonList(ExpenseType.SPECIAL), Collections.emptySet()));
                startActivity(intent);
                break;
            case R.id.navigation_yearly_report:
                Intent yearlyReportIntent = new Intent(ExpenseMainActivity.this, YearlyExpenseReportActivity.class);
                yearlyReportIntent.putExtra(INTENT_YEAR, year);

                startActivity(yearlyReportIntent);
                break;
            case R.id.navigation_expense_history:
                expenseHistoryRequested();
                break;
            case R.id.navigation_database_dump:
                dbDumpRequested();
                break;
            case R.id.navigation_database_restore:
                dbRestoreRequested();
                break;
        }
        return true;
    }

    private void dbSearchRequested() {
        expenseTypeSelectionDialog.show(lastUsedGlobalFilters,
                ExpenseType.orderedExpenseTypes(),
                filters -> {
                    lastUsedGlobalFilters = filters;

                    final Intent searchDatabaseIntent = new Intent(ExpenseMainActivity.this, ExpenseSearchResultActivity.class);
                    searchDatabaseIntent.putExtra(INTENT_FILTERS, filters);
                    startActivityForResult(searchDatabaseIntent, ExpenseSearchResultActivity.EXPENSE_SEARCH_RESULT_ACTIVITY_ID);
                });
    }

    private void expenseHistoryRequested() {
        expenseTypeSelectionDialog.show(null,
                Arrays.asList(ExpenseType.values()),
                filters -> {
                    final Intent intent = new Intent(ExpenseMainActivity.this, ExpenseHistoryChartActivity.class);
                    intent.putExtra(INTENT_FILTERS, (Serializable) filters.getExpenseTypes());
                    startActivity(intent);
                });
    }

    private void dbDumpRequested() {
        try {
            final List<Expense> allExpenses = expenseDao.fetchExpenses(null, null);
            FileUtil.exportExpenses(allExpenses);
            Toast.makeText(this, R.string.expenseListActivityExportSuccessful, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void dbRestoreRequested() {
        importFileSelectionDialog.show(fileName -> {
            try {
                final List<Expense> expenses = FileUtil.importExpenses(fileName);
                expenseDao.restoreExpenses(expenses);
                repopulateExpenseList(year, month);
            } catch (IOException e) {
                Toast.makeText(ExpenseMainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case AddEditExpenseActivity.ADD_EXPENSE_ACTIVITY_ID:
                processAddEditExpenseResult(resultCode, data);
                break;
            case ExpenseSearchResultActivity.EXPENSE_SEARCH_RESULT_ACTIVITY_ID:
                processSearchResultActivityResult(resultCode, data);
                break;
        }
    }

    private void processAddEditExpenseResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Expense expense = (Expense) data.getSerializableExtra(INTENT_EXPENSE);
            Log.i(APPLICATION_NAME, expense.toString());

            final Long expenseId = expense.getId();
            boolean newExpenseAddedThisMonth = false;

            if (expenseId == null) { // add new expense
                expenseDao.persistExpense(expense);

                if (isInSameMonth(expense.getTime())) {
                    clearFilters();

                    if (expense.getExpenseType() != ExpenseType.SPECIAL) {
                        expenseListAdapter.addExpense(expense);
                        newExpenseAddedThisMonth = true;
                    }
                }
            } else { // update existing expense
                expenseDao.updateExpense(expense);
                if (!isInSameMonth(expense.getTime())) {
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
    }

    @SuppressWarnings("unchecked")
    private void processSearchResultActivityResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            List<Expense> changedExpenses = (List<Expense>) data.getSerializableExtra(INTENT_CHANGED_EXPENSES);
            expenseListAdapter.updateExpenses(changedExpenses);
            expenseListAdapter.notifyDataSetChanged();
        }
    }

    private boolean isInSameMonth(final long time) {
        calendar.setTimeInMillis(time);
        return calendar.get(Calendar.YEAR) == year && (calendar.get(Calendar.MONTH) + 1) == month;
    }

    @Override
    public void onDateSet(final DatePicker datePicker, final int year, final int monthOfYear, final int dayOfMonth) {
        int adjustedMonthOfYear = monthOfYear + 1;
        if (this.year != year || this.month != adjustedMonthOfYear) {
            this.month = adjustedMonthOfYear;
            this.year = year;
            repopulateExpenseList(year, adjustedMonthOfYear);
        }
    }

    private void repopulateExpenseList(final int year, final int monthOfYear) {
        new ExpenseLoaderTask(expenseDao, expenses -> {
            clearFilters();
            Collections.sort(expenses, mapSortingMenuItemToComparator.get(1));
            expenseListAdapter.updateExpenseList(expenses);
            updateTotal();
            updateMonthButtonLabel();
            expenseList.smoothScrollToPosition(expenseListAdapter.getCount());

            updatePieData();
        }).execute(new MonthWrapper(year, monthOfYear));
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
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.YEAR, year);
        changeMonthButton.setText(dateFormat.format(calendar.getTime()));
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
        importFileSelectionDialog.dismiss();
    }
}
