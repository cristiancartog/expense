package ro.pandemonium.expense.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.view.adapter.ExpenseTypeSpinnerAdapter;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_DISPLAY;


public class AddEditExpenseActivity extends Activity
        implements View.OnClickListener, TextWatcher, DatePickerDialog.OnDateSetListener {

    public static final int ADD_EXPENSE_ACTIVITY_ID = 2;
    private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat(Constants.ADD_ACTIVITY_FORMAT_PATTERN);

    private ExpenseTypeSpinnerAdapter expenseTypeSpinnerAdapter;
    private Spinner spinner;
    private EditText valueField;
    private Button dateButton;
    private EditText commentField;

    private Button saveExpenseButton;

    private final Calendar calendar = Calendar.getInstance();
    private final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_PATTERN_DISPLAY, Locale.getDefault());
    private Long expenseId;
    private long expenseTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_expense);

        valueField = (EditText) findViewById(R.id.addExpenseValue);
        dateButton = (Button) findViewById(R.id.addExpenseDate);
        spinner = (Spinner) findViewById(R.id.addExpenseSpinner);
        commentField = (EditText) findViewById(R.id.addExpenseComment);
        saveExpenseButton = (Button) findViewById(R.id.saveExpenseButton);

        saveExpenseButton.setEnabled(false);
        saveExpenseButton.setOnClickListener(this);
        valueField.addTextChangedListener(this);

        final Intent intent = getIntent();
        @SuppressWarnings("unchecked") final Map<ExpenseType, Integer> mapExpenseTypeToCount = (Map<ExpenseType, Integer>) intent.getSerializableExtra(Constants.INTENT_EXPENSE_COUNT_MAP);
        final Expense expense = (Expense) intent.getSerializableExtra(Constants.INTENT_EXPENSE_EXPENSE_TO_EDIT);

        expenseTypeSpinnerAdapter = new ExpenseTypeSpinnerAdapter();
        expenseTypeSpinnerAdapter.setExpenseTypeMap(mapExpenseTypeToCount);
        spinner.setAdapter(expenseTypeSpinnerAdapter);

        setExpense(expense);
    }

    private void setExpense(final Expense expense) {
        if (expense != null) {
            final int position = expenseTypeSpinnerAdapter.getPosition(expense.getExpenseType());
            if (position < 0) {
                Toast.makeText(this, "Expense type not valid", Toast.LENGTH_LONG).show();
                return;
            }

            expenseId = expense.getId();

            valueField.setText(NUMBER_FORMATTER.format(expense.getValue()));
            spinner.setSelection(position);
            calendar.setTime(new Date(expense.getTime()));
            dateButton.setText(expense.getFormattedDate(dateFormat));
            commentField.setText(expense.getComment());

            expenseTime = expense.getTime();
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.saveExpenseButton:
                final Intent intent = getIntent();

                final Expense expense = new Expense();
                expense.setId(expenseId);
                expense.setExpenseType((ExpenseType) spinner.getSelectedItem());
                expense.setValue(Double.parseDouble(valueField.getText().toString()));
                expense.setDate(new Date(expenseTime));
                expense.setComment(commentField.getText().toString());

                intent.putExtra(Constants.INTENT_EXPENSE, expense);

                setResult(RESULT_OK, intent);
                finish();
                break;
            case R.id.addExpenseDate:
                calendar.setTime(new Date(expenseTime));

                new DatePickerDialog(this, this,
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                        .show();
                break;
        }
    }

    @Override
    public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthOfYear);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        Date date = calendar.getTime();
        expenseTime = date.getTime();
        dateButton.setText(dateFormat.format(date));
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
        saveExpenseButton.setEnabled(valueField.getText().length() > 0);
    }
}