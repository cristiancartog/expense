package ro.pandemonium.expense.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.BarChartActivity;
import ro.pandemonium.expense.activity.dialog.ImportFileSelectionDialog;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.model.MonthWrapper;
import ro.pandemonium.expense.task.ExpenseLoaderTask;
import ro.pandemonium.expense.util.FileUtil;

import static ro.pandemonium.expense.Constants.APPLICATION_NAME;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.Constants.INTENT_CHANGED_EXPENSES;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE;
import static ro.pandemonium.expense.Constants.INTENT_EXPENSE_COUNT_MAP;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;

public class ExpenseMainActivity extends AbstractExpenseListActivity
        implements View.OnClickListener,
        DatePickerDialog.OnDateSetListener,
        AdapterView.OnItemClickListener,
        View.OnLongClickListener {

    private final Calendar calendar = Calendar.getInstance();
    private int year;
    private int monthOfYear;

    private Button changeMonthButton;
    private final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault());

    // dialogs
    private ImportFileSelectionDialog importFileSelectionDialog;
    private Filters lastUsedGlobalFilters;

    public ExpenseMainActivity() {
    }

    @Override
    int getLayoutId() {
        return R.layout.expense_list;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        expenseList = (ListView) findViewById(R.id.expansesListView);
        expenseList.setAdapter(expenseListAdapter);
        expenseList.setOnItemClickListener(this);

        totalTextView = (TextView) findViewById(R.id.expenseListTotal);
        changeMonthButton = (Button) findViewById(R.id.expenseListChangeMonthButton);
        filtersButton = (Button) findViewById(R.id.expenseListFiltersButton);
        filtersButton.setOnLongClickListener(this);

        importFileSelectionDialog = new ImportFileSelectionDialog(this);

        year = calendar.get(Calendar.YEAR);
        monthOfYear = calendar.get(Calendar.MONTH) + 1;

        repopulateExpenseList(year, monthOfYear);
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.expenseListChangeMonthButton:
                new DatePickerDialog(this, this, year, monthOfYear - 1, 1).show();
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
                monthOfYear--;
                if (monthOfYear < 1) {
                    monthOfYear = 12;
                    year--;
                }

                repopulateExpenseList(year, monthOfYear);
                break;

            case R.id.expenseListNextMonth:
                monthOfYear++;
                if (monthOfYear > 12) {
                    monthOfYear = 1;
                    year++;
                }

                repopulateExpenseList(year, monthOfYear);
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
        getMenuInflater().inflate(R.menu.expense_main_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(final int featureId, final MenuItem item) {
        Log.i(APPLICATION_NAME, item.getTitle().toString());

        final int menuItemId = item.getItemId();

        switch (menuItemId) {
            case R.id.main_menu_chart_pie:
                showPieChart();
                break;

            case R.id.main_menu_chart_line:
                break;

            case R.id.main_menu_chart_bar:
                expenseTypeSelectionDialog.show(null,
                        Arrays.asList(ExpenseType.values()),
                        filters -> {
                            final Intent intent = new Intent(ExpenseMainActivity.this, BarChartActivity.class);
                            intent.putExtra(INTENT_FILTERS, (Serializable) filters.getExpenseTypes());
                            startActivity(intent);
                        });
                break;

            case R.id.main_menu_database_import:
                importFileSelectionDialog.show(fileName -> {
                    try {
                        final List<Expense> expenses = FileUtil.importExpenses(fileName);
                        expenseDao.restoreExpenses(expenses);
                        repopulateExpenseList(year, monthOfYear);
                    } catch (IOException e) {
                        Toast.makeText(ExpenseMainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
                break;

            case R.id.main_menu_database_export:
                try {
                    final List<Expense> allExpenses = expenseDao.fetchExpenses(null, null);
                    FileUtil.exportExpenses(allExpenses);
                    Toast.makeText(this, R.string.expenseListActivityExportSuccessful, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.main_menu_search_db:
                expenseTypeSelectionDialog.show(lastUsedGlobalFilters,
                        ExpenseType.orderedExpenseTypes(),
                        filters -> {
                            lastUsedGlobalFilters = filters;

                            final Intent searchDatabaseIntent = new Intent(ExpenseMainActivity.this, ExpenseSearchResultActivity.class);
                            searchDatabaseIntent.putExtra(INTENT_FILTERS, filters);
                            startActivityForResult(searchDatabaseIntent, ExpenseSearchResultActivity.EXPENSE_SEARCH_RESULT_ACTIVITY_ID);
                        });
                break;

            case R.id.main_menu_special_expenses:
                startActivity(new Intent(ExpenseMainActivity.this, SpecialExpensesActivity.class));
                break;

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
                if (!isInSameMonth(expense.getTime())) {
                    expenseListAdapter.deleteExpense(expense);
                }
                expenseDao.updateExpense(expense);
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
        return calendar.get(Calendar.YEAR) == year && (calendar.get(Calendar.MONTH) + 1) == monthOfYear;
    }

    @Override
    public void onDateSet(final DatePicker datePicker, final int year, final int monthOfYear, final int dayOfMonth) {
        int adjustedMonthOfYear = monthOfYear + 1;
        if (this.year != year || this.monthOfYear != adjustedMonthOfYear) {
            this.monthOfYear = adjustedMonthOfYear;
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
        }).execute(new MonthWrapper(year, monthOfYear));
    }

    private void updateMonthButtonLabel() {
        calendar.set(Calendar.MONTH, monthOfYear - 1);
        calendar.set(Calendar.YEAR, year);
        changeMonthButton.setText(dateFormat.format(calendar.getTime()));
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        addEditExpense(position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        expenseDao.closeDatabase();
        importFileSelectionDialog.dismiss();
    }
}
