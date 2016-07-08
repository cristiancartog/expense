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
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.BarChartActivity;
import ro.pandemonium.expense.activity.chart.ChartIntentFactory;
import ro.pandemonium.expense.activity.dialog.ExpenseTypeSelectionDialog;
import ro.pandemonium.expense.activity.dialog.ImportFileSelectionDialog;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.util.FileUtil;

public class ExpenseMainActivity extends AbstractExpenseListActivity
        implements View.OnClickListener, DatePickerDialog.OnDateSetListener, AdapterView.OnItemClickListener, View.OnLongClickListener {

    final Calendar cal = Calendar.getInstance();
    private int year;
    private int monthOfYear;

    private Button changeMonthButton;
    private DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_MONTH, Locale.getDefault());

    // dialogs
    private ImportFileSelectionDialog importFileSelectionDialog;
    private Filters lastUsedGlobalFilters;

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

        year = cal.get(Calendar.YEAR);
        monthOfYear = cal.get(Calendar.MONTH) + 1;

        repopulateExpenseList(year, monthOfYear);
        sortExpenses(1);
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.expenseListChangeMonthButton:
                new DatePickerDialog(this, this, year, monthOfYear - 1, 1).show();
                break;

            case R.id.addExpenseButton:
                final Intent addExpenseIntent = new Intent(this, AddEditExpenseActivity.class);
                addExpenseIntent.putExtra(Constants.INTENT_EXPENSE_COUNT_MAP, expenseListAdapter.getExpenseTypes());
                startActivityForResult(addExpenseIntent, AddEditExpenseActivity.ADD_EXPENSE_ACTIVITY_ID);
                break;

            case R.id.expenseListFiltersButton:
                showFiltersDialog();
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
        Log.i(Constants.APPLICATION_NAME, item.getTitle().toString());

        final int menuItemId = item.getItemId();

        switch (menuItemId) {
            case R.id.main_menu_chart_pie:
                showPieChart();
                break;

            case R.id.main_menu_chart_line:
                expenseTypeSelectionDialog.show(null,
                        expenseListAdapter.expenseTypeSet(),
                        new ExpenseTypeSelectionDialog.Callback() {
                            @Override
                            public void expenseTypesSelected(final Filters filters) {
                                final List<Expense> expenses = expenseDao.fetchExpenses(filters.getExpenseTypes());
                                final Intent intent = ChartIntentFactory.createLineChartIntent(ExpenseMainActivity.this, expenses);
                                intent.putExtra(Constants.INTENT_FILTERS, (Serializable) filters.getExpenseTypes());
                                startActivity(intent);
                            }
                        });
                break;

            case R.id.main_menu_chart_bar:
                expenseTypeSelectionDialog.show(null,
                        Arrays.asList(ExpenseType.values()),
                        new ExpenseTypeSelectionDialog.Callback() {
                            @Override
                            public void expenseTypesSelected(final Filters filters) {
                                final Intent intent = new Intent(ExpenseMainActivity.this, BarChartActivity.class);
                                intent.putExtra(Constants.INTENT_FILTERS, (Serializable) filters.getExpenseTypes());
                                startActivity(intent);
                            }
                        });
                break;

            case R.id.main_menu_database_import:
                importFileSelectionDialog.show(new ImportFileSelectionDialog.Callback() {
                    @Override
                    public void fileSelected(final String fileName) {
                        try {
                            final List<Expense> expenses = FileUtil.importExpenses(fileName);
                            expenseDao.restoreExpenses(expenses);
                            repopulateExpenseList(year, monthOfYear);
                        } catch (IOException e) {
                            Toast.makeText(ExpenseMainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
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
                        new ExpenseTypeSelectionDialog.Callback() {
                            @Override
                            public void expenseTypesSelected(final Filters filters) {
                                lastUsedGlobalFilters = filters;

                                final Intent searchDatabaseIntent = new Intent(ExpenseMainActivity.this, ExpenseSearchResultActivity.class);
                                searchDatabaseIntent.putExtra(Constants.INTENT_FILTERS, filters);
                                startActivityForResult(searchDatabaseIntent, ExpenseSearchResultActivity.EXPENSE_SEARCH_RESULT_ACTIVITY_ID);
                            }
                        });
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
            final Expense expense = (Expense) data.getSerializableExtra(Constants.INTENT_EXPENSE);
            Log.i(Constants.APPLICATION_NAME, expense.toString());

            final Long expenseId = expense.getId();
            boolean newExpenseAddedThisMonth = false;

            if (expenseId == null) { // add new expense
                if (isInSameMonth(expense.getDate())) {
                    if (expenseListAdapter.isFiltered()) {
                        expenseListAdapter.clearFilters();
                    }
                    expenseListAdapter.addExpense(expense);
                    newExpenseAddedThisMonth = true;
                }
                expenseDao.persistExpense(expense);
            } else { // update existing expense
                if (isInSameMonth(expense.getDate())) {
                    expenseListAdapter.updateExpense(expense);
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
            List<Expense> changedExpenses = (List<Expense>) data.getSerializableExtra(Constants.INTENT_CHANGED_EXPENSES);
            expenseListAdapter.updateExpenses(changedExpenses);
            expenseListAdapter.notifyDataSetChanged();
        }
    }

    private boolean isInSameMonth(final Date date) {
        cal.setTime(date);
        return cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == monthOfYear;
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
        clearFilters();
        final List<Expense> expensesFromDb = expenseDao.fetchExpenses(year, monthOfYear);
        expenseListAdapter.updateExpenseList(expensesFromDb);
        updateTotal();
        updateMonthButtonLabel();
        expenseList.smoothScrollToPosition(expenseListAdapter.getCount());
    }

    private void updateMonthButtonLabel() {
        cal.set(Calendar.MONTH, monthOfYear - 1);
        cal.set(Calendar.YEAR, year);
        changeMonthButton.setText(dateFormat.format(cal.getTime()));
    }

    @Override
    public void onItemClick(final AdapterView<?> adapterView, final View view, final int position, final long id) {
        addEditExpense(position);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        expenseDao.closeDatabase();
        importFileSelectionDialog.dismiss();
    }
}
