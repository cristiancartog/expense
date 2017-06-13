package ro.pandemonium.expense.view.adapter;

import android.graphics.Color;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ro.pandemonium.expense.Constants;
import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.view.ExpenseTypeColor;

public class ExpenseListAdapter extends BaseAdapter {

    private final List<Expense> expenses = new LinkedList<>();
    private final List<Expense> backupExpenses = new LinkedList<>();
    private final Map<Object, Expense> mapDeleteButtonTagToExpense = new HashMap<>();
    private final LongSparseArray<Expense> mapExpenseIdToExpense = new LongSparseArray<>();

    private Filters filters;

    private final NumberFormat numberFormatter = new DecimalFormat(Constants.NUMBER_FORMAT_PATTERN);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT_PATTERN_DISPLAY, Locale.US);

    @Override
    public int getCount() {
        return expenses.size();
    }

    @Override
    public Object getItem(final int index) {
        return expenses.get(index);
    }

    @Override
    public long getItemId(final int index) {
        return index;
    }

    @Override
    public View getView(final int index, View view, final ViewGroup viewGroup) {
        final Expense expense = expenses.get(index);

        if (view == null) {
            final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
            view = layoutInflater.inflate(R.layout.expense_list_item, viewGroup, false);
        }

        view.setBackgroundColor(index % 2 == 0 ? view.getResources().getColor(R.color.menu_item_odd) : Color.BLACK);

        final ImageButton deleteButton = (ImageButton) view.findViewById(R.id.expenseListItemDelete);
        deleteButton.setTag(index);
        mapDeleteButtonTagToExpense.put(deleteButton.getTag(), expense);

        final TextView currentNr = (TextView) view.findViewById(R.id.expenseListItemNumber);
        currentNr.setText(String.format(Locale.getDefault(), "%d.", index + 1));

        final TextView expenseTypeView = (TextView) view.findViewById(R.id.expenseListItemType);
        final ExpenseType expenseType = expense.getExpenseType();
        expenseTypeView.setText(expenseType.getTextResource());
        expenseTypeView.setTextColor(ExpenseTypeColor.color(expenseType));

        final TextView descriptionView = (TextView) view.findViewById(R.id.expenseListItemDescription);
        descriptionView.setText(expense.getComment());

        final TextView valueView = (TextView) view.findViewById(R.id.expenseListItemValue);
        valueView.setText(numberFormatter.format(expense.getValue()));

        final TextView dateView = (TextView) view.findViewById(R.id.expenseListItemDate);
        dateView.setText(expense.getFormattedDate(dateFormat));

        return view;
    }

    public void addExpense(final Expense expense) {
        expenses.add(expense);
        mapExpenseIdToExpense.put(expense.getId(), expense);
    }

    /**
     * Updates the data for the current expenses with what is provided.
     * @param expenses - expenses with new data
     */
    public void updateExpenses(final List<Expense> expenses) {
        for (Expense expense : expenses) {
            updateExpense(expense);
        }
    }

    public void updateExpense(final Expense expense) {
        final Expense localExpense = mapExpenseIdToExpense.get(expense.getId());
        if (localExpense != null) {
            localExpense.setExpenseType(expense.getExpenseType());
            localExpense.setValue(expense.getValue());
            localExpense.setDate(new Date(expense.getTime()));
            localExpense.setComment(expense.getComment());
        }
    }

    /**
     * Replaces current expenses with the ones provided.
     * @param newExpenses - the new expenses
     */
    public void updateExpenseList(final List<Expense> newExpenses) {
        if (isFiltered()) {
            clearFilters();
        }
        expenses.clear();
        expenses.addAll(newExpenses);

        mapExpenseIdToExpense.clear();
        for (Expense expense : newExpenses) {
            mapExpenseIdToExpense.put(expense.getId(), expense);
        }

        notifyDataSetChanged();
    }

    public List<Expense> getExpenses(final boolean includeUnfiltered) {
        final List<Expense> expensesToReturn = new ArrayList<>();
        expensesToReturn.addAll(includeUnfiltered ? (isFiltered() ? backupExpenses : expenses) : expenses);
        return expensesToReturn;
    }

    public Map<ExpenseType, Integer> getExpenseTypes() {
        final Map<ExpenseType, Integer> mapExpenseTypesToCount = new HashMap<>();

        for (Expense expense : expenses) {
            final ExpenseType expenseType = expense.getExpenseType();
            final Integer count = mapExpenseTypesToCount.get(expenseType);
            if (count == null) {
                mapExpenseTypesToCount.put(expenseType, 1);
            } else {
                mapExpenseTypesToCount.put(expenseType, count + 1);
            }
        }

        return mapExpenseTypesToCount;
    }

    public Expense deleteExpenseForTag(final Object tag) {
        final Expense expense = mapDeleteButtonTagToExpense.get(tag);

        expenses.remove(expense);
        if (isFiltered()) {
            backupExpenses.remove(expense);
        }
        mapDeleteButtonTagToExpense.remove(tag);

        notifyDataSetChanged();

        return expense;
    }

    public void deleteExpense(final Expense expense) {
        expenses.remove(expense);
        if (isFiltered()) {
            backupExpenses.remove(expense);
        }
    }

    public void sortExpenses(final Comparator<Expense> comparator) {
        Collections.sort(expenses, comparator);
        notifyDataSetChanged();
    }

    public boolean isFiltered() {
        return filters != null;
    }

    public void filterBy(final Filters filters) {
        if (isFiltered()) {
            expenses.clear();
            expenses.addAll(backupExpenses);
        } else {
            backupExpenses.addAll(expenses);
        }

        this.filters = filters;

        final Iterator<Expense> expensesIterator = expenses.iterator();
        while (expensesIterator.hasNext()) {
            if (shouldFilter(expensesIterator.next())) {
                expensesIterator.remove();
            }
        }

        notifyDataSetChanged();
    }

    public Set<ExpenseType> expenseTypeSet() {
        Set<ExpenseType> expenseTypeSet = new HashSet<>();

        List<Expense> expensesSource = isFiltered() ? backupExpenses : this.expenses;
        for (Expense expense : expensesSource) {
            expenseTypeSet.add(expense.getExpenseType());
        }

        return expenseTypeSet;
    }

    private boolean shouldFilter(Expense expense) {
        List<ExpenseType> expenseTypes = filters.getExpenseTypes();
        return !expenseTypes.contains(expense.getExpenseType()) && !matchesComment(expense.getComment());
    }

    private boolean matchesComment(String comment) {
        Set<String> comments = filters.getComments();

        for (String filterComment : comments) {
            if (comment.contains(filterComment)) {
                return true;
            }
        }

        return false;
    }

    public void clearFilters() {
        expenses.clear();
        expenses.addAll(backupExpenses);
        backupExpenses.clear();
        filters = null;
        notifyDataSetChanged();
    }
}
