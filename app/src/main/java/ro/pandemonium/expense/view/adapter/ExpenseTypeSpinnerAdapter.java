package ro.pandemonium.expense.view.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.ExpenseType;

import java.util.*;

public class ExpenseTypeSpinnerAdapter extends BaseAdapter {

    private final List<ExpenseType> expenseTypes = ExpenseType.orderedExpenseTypes();
    private Map<ExpenseType, Integer> mapExpenseTypeToCount = new HashMap<>();

    public void setExpenseTypeMap(final Map<ExpenseType, Integer> expenseTypeMap) {
        this.mapExpenseTypeToCount = expenseTypeMap;
    }

    @Override
    public int getCount() {
        return expenseTypes.size();
    }

    @Override
    public Object getItem(final int index) {
        return expenseTypes.get(index);
    }

    @Override
    public long getItemId(final int index) {
        return index;
    }

    @Override
    public View getView(final int index, final View view, final ViewGroup viewGroup) {
       return getViewImpl(index, view, viewGroup, R.layout.expense_type_spinner_item);
    }

    @Override
    public View getDropDownView(int index, View view, ViewGroup viewGroup) {
       return getViewImpl(index, view, viewGroup, R.layout.expense_type_spinner_list_item);
    }

    public int getPosition (final ExpenseType expenseType) {
        int position = -1;

        for (int i = 0; i < expenseTypes.size(); i++) {
            if (expenseTypes.get(i) == expenseType) {
                position = i;
                break;
            }
        }

        return position;
    }

    private View getViewImpl (final int index, final View view, final ViewGroup viewGroup, final int resourceId) {
        TextView checkedTextView = (TextView) view;
        if (checkedTextView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
            checkedTextView = (TextView) layoutInflater.inflate(resourceId, viewGroup, false);
        }

        final ExpenseType expenseType = expenseTypes.get(index);
        final Integer count = mapExpenseTypeToCount.get(expenseType);
        final boolean useGrayText = count != null && count >= expenseType.getMaxOccurrences();
        checkedTextView.setText(expenseType.getTextResource());
        checkedTextView.setTextColor(useGrayText ? Color.GRAY : Color.WHITE);
        checkedTextView.setPadding(expenseType.getMaxOccurrences() == 1 ? 30 : 10, 0, 0, 0);
        checkedTextView.setTypeface(null, expenseType.getMaxOccurrences() == 1 ? Typeface.BOLD: Typeface.NORMAL);

        return checkedTextView;
    }
}
