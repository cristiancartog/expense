package ro.pandemonium.expense.view.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.comparator.ExpenseTypeOrderComparator;

public class ExpenseTypeListAdapter extends BaseAdapter {

    private List<ExpenseType> expenseTypes = Collections.emptyList();

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
        return getViewImpl(index, view, viewGroup);
    }

    @Override
    public View getDropDownView(int index, View view, ViewGroup viewGroup) {
        return getViewImpl(index, view, viewGroup);
    }

    public void setAvailableExpenseTypes(Collection<ExpenseType> expenseTypes) {
        this.expenseTypes = new ArrayList<>(expenseTypes);
        Collections.sort(this.expenseTypes, new ExpenseTypeOrderComparator());
    }

    private View getViewImpl(final int index, final View view, final ViewGroup viewGroup) {
        CheckedTextView checkedTextView = (CheckedTextView) view;

        if (checkedTextView == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
            checkedTextView = (CheckedTextView) layoutInflater.inflate(android.R.layout.simple_list_item_multiple_choice, viewGroup, false);
        }

        final ExpenseType expenseType = expenseTypes.get(index);
        checkedTextView.setText(expenseType.getTextResource());
        checkedTextView.setBackgroundColor(Color.rgb(255, 244, 244));
        checkedTextView.setTextColor(expenseType.getMaxOccurrences() == 1 ? Color.DKGRAY : Color.BLACK);
        checkedTextView.setTypeface(null, expenseType.getMaxOccurrences() == 1 ? Typeface.BOLD : Typeface.NORMAL);

        return checkedTextView;
    }
}
