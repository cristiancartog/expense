package ro.pandemonium.expense.activity.chart;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.ExpenseType;

import static ro.pandemonium.expense.Constants.INTEN_EXPENSE_TYPE;

public class YearlyComparisonChartActivity extends Activity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.yearly_comparison_chart_activity);

        ExpenseType expenseType = (ExpenseType) getIntent().getSerializableExtra(INTEN_EXPENSE_TYPE);

        TextView expenseTypeText = (TextView) findViewById(R.id.yearlyComparisonExpenseType);
        expenseTypeText.setText(expenseType.getTextResource());
    }

}
