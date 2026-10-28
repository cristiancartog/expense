package ro.pandemonium.expense.view.adapter;

import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_DISPLAY;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import ro.pandemonium.expense.view.ExpenseTypeUi;

public class ExpenseListAdapter extends BaseAdapter {

    private final List<Expense> expenses = new LinkedList<>();
    private final List<Expense> backupExpenses = new LinkedList<>();
    private final Map<Object, Expense> mapDeleteButtonTagToExpense = new HashMap<>();
    private final LongSparseArray<Expense> mapExpenseIdToExpense = new LongSparseArray<>();

    private Filters filters;

    private final NumberFormat numberFormatter = new DecimalFormat(Constants.NUMBER_FORMAT_PATTERN);
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_DISPLAY, Locale.US);

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

        view.setBackgroundColor(index % 2 == 0
                ? view.getContext().getColor(R.color.menu_item_odd)
                : Color.BLACK);

        final ImageButton deleteButton = view.findViewById(R.id.expenseListItemDelete);
        deleteButton.setTag(index);
        mapDeleteButtonTagToExpense.put(deleteButton.getTag(), expense);

        final TextView currentNr = view.findViewById(R.id.expenseListItemNumber);
        currentNr.setText(String.format(Locale.getDefault(), "%d.", index + 1));

        final TextView expenseTypeView = view.findViewById(R.id.expenseListItemType);
        final ExpenseType expenseType = expense.getExpenseType();
        expenseTypeView.setText(ExpenseTypeUi.getTextResource(expenseType));
        expenseTypeView.setTextColor(ExpenseTypeUi.getColor(expenseType));

        final TextView descriptionView = view.findViewById(R.id.expenseListItemDescription);
        descriptionView.setText(expense.getComment());

        final TextView valueView = view.findViewById(R.id.expenseListItemValue);
        valueView.setText(numberFormatter.format(expense.getValue()));

        final TextView dateView = view.findViewById(R.id.expenseListItemDate);
        dateView.setText(expense.getFormattedDate(dateFormatter));

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
            localExpense.setDate(expense.getDate());
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
        return new ArrayList<>(includeUnfiltered
                ? (isFiltered()
                   ? backupExpenses
                   : expenses)
                : expenses);
    }

    public Map<ExpenseType, Integer> getExpenseTypes() {
        final Map<ExpenseType, Integer> mapExpenseTypesToCount = new HashMap<>();

        for (Expense expense : expenses) {
            final ExpenseType expenseType = expense.getExpenseType();
            mapExpenseTypesToCount.merge(expenseType, 1, Integer::sum);
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
        expenses.sort(comparator);
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

        expenses.removeIf(expense -> {
            List<ExpenseType> expenseTypes = filters.expenseTypes();
            return !expenseTypes.contains(expense.getExpenseType())
                    && !matchesComment(expense.getComment());
        });

        notifyDataSetChanged();
    }

    private boolean matchesComment(String comment) {
        Set<String> comments = filters.comments();

        for (String filterComment : comments) {
            if (comment.contains(filterComment)) {
                return true;
            }
        }

        return false;
    }

    public Set<ExpenseType> expenseTypeSet() {
        Set<ExpenseType> expenseTypeSet = new HashSet<>();

        List<Expense> expensesSource = isFiltered() ? backupExpenses : this.expenses;
        for (Expense expense : expensesSource) {
            expenseTypeSet.add(expense.getExpenseType());
        }

        return expenseTypeSet;
    }

    public void clearFilters() {
        expenses.clear();
        expenses.addAll(backupExpenses);
        backupExpenses.clear();
        filters = null;
        notifyDataSetChanged();
    }

}
