package ro.pandemonium.expense.activity;

import static java.time.LocalDate.now;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_DISPLAY;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.util.SystemBarInsets;
import ro.pandemonium.expense.view.adapter.ExpenseTypeSpinnerAdapter;


public class AddEditExpenseActivity extends AppCompatActivity
        implements TextWatcher, DatePickerDialog.OnDateSetListener {

    private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat(Constants.ADD_ACTIVITY_FORMAT_PATTERN);

    private ExpenseTypeSpinnerAdapter expenseTypeSpinnerAdapter;
    private Spinner spinner;
    private EditText valueField;
    private Button dateButton;
    private EditText commentField;

    private Button saveExpenseButton;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_DISPLAY, Locale.getDefault());
    private Long expenseId;
    private LocalDate expenseDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_expense_appbar);
        SystemBarInsets.keepContentInsideSystemBars(this);

        valueField = findViewById(R.id.addExpenseValue);
        dateButton = findViewById(R.id.addExpenseDate);
        spinner = findViewById(R.id.addExpenseSpinner);
        commentField = findViewById(R.id.addExpenseComment);
        saveExpenseButton = findViewById(R.id.saveExpenseButton);

        saveExpenseButton.setEnabled(false);
        saveExpenseButton.setOnClickListener(this::saveExpense);
        valueField.addTextChangedListener(this);
        dateButton.setOnClickListener(this::addExpenseDate);

        final Intent intent = getIntent();
        @SuppressWarnings("unchecked")
        final Map<ExpenseType, Integer> mapExpenseTypeToCount = (Map<ExpenseType, Integer>) intent.getSerializableExtra(Constants.INTENT_EXPENSE_COUNT_MAP, HashMap.class);
        final Expense expense = intent.getSerializableExtra(Constants.INTENT_EXPENSE_EXPENSE_TO_EDIT, Expense.class);

        expenseTypeSpinnerAdapter = new ExpenseTypeSpinnerAdapter();
        expenseTypeSpinnerAdapter.setExpenseTypeMap(mapExpenseTypeToCount);
        spinner.setAdapter(expenseTypeSpinnerAdapter);

        setExpense(expense);
    }

    private void setExpense(final Expense expense) {
        if (expense != null) {
            ((Toolbar) findViewById(R.id.addEditExpenseToolbar)).setTitle(R.string.addEditExpenseEditTitle);

            final int position = expenseTypeSpinnerAdapter.getPosition(expense.getExpenseType());
            if (position < 0) {
                Toast.makeText(this, "Expense type not valid", Toast.LENGTH_LONG).show();
                return;
            }

            expenseId = expense.getId();

            valueField.setText(NUMBER_FORMATTER.format(expense.getValue()));
            spinner.setSelection(position);
            dateButton.setText(dateFormatter.format(expense.getDate()));
            commentField.setText(expense.getComment());

            expenseDate = expense.getDate();
        } else {
            expenseDate = now();
            dateButton.setText(dateFormatter.format(expenseDate));
        }
    }

    private void saveExpense(final View view) {
        final Intent intent = getIntent();

        final Expense expense = new Expense();
        expense.setId(expenseId);
        expense.setExpenseType((ExpenseType) spinner.getSelectedItem());
        expense.setValue(Double.parseDouble(valueField.getText().toString()));
        expense.setDate(expenseDate);
        expense.setComment(commentField.getText().toString());

        intent.putExtra(Constants.INTENT_EXPENSE, expense);

        setResult(RESULT_OK, intent);
        finish();
    }

    private void addExpenseDate(final View view) {
        new DatePickerDialog(this, this, expenseDate.getYear(), expenseDate.getMonth().ordinal(), expenseDate.getDayOfMonth())
                .show();
    }

    @Override
    public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
        expenseDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth);
        dateButton.setText(expenseDate.format(dateFormatter));
    }

    @Override
    public void onTextChanged(final CharSequence charSequence, final int start, final int before, final int count) {
        // nothing
    }

    @Override
    public void beforeTextChanged(final CharSequence charSequence, final int start, final int count, final int after) {
        // nothing
    }

    @Override
    public void afterTextChanged(final Editable editable) {
        saveExpenseButton.setEnabled(!valueField.getText().isEmpty());
    }
}
