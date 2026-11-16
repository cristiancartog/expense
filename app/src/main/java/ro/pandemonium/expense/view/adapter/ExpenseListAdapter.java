package ro.pandemonium.expense.view.adapter;

import static java.util.Objects.requireNonNull;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_DISPLAY;

import android.graphics.Color;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

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
import java.util.Objects;
import java.util.Set;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.util.ExpenseFormatUtil;
import ro.pandemonium.expense.view.ExpenseTypeUi;

public class ExpenseListAdapter extends BaseAdapter {

    private final List<Expense> expenses = new LinkedList<>();
    private final List<Expense> allExpenses = new LinkedList<>();
    private final LongSparseArray<Expense> mapExpenseIdToExpense = new LongSparseArray<>();

    private Filters filters;
    private Comparator<Expense> comparator = (first, second) -> 0;

    private final NumberFormat numberFormatter = ExpenseFormatUtil.amountFormatter();
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
        deleteButton.setTag(expense.getId());

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
        allExpenses.add(expense);
        mapExpenseIdToExpense.put(expense.getId(), expense);
        refreshVisibleExpenses();
    }

    public void updateExpense(final Expense expense) {
        final Expense localExpense = mapExpenseIdToExpense.get(expense.getId());
        if (localExpense != null) {
            localExpense.setExpenseType(expense.getExpenseType());
            localExpense.setValue(expense.getValue());
            localExpense.setDate(expense.getDate());
            localExpense.setComment(expense.getComment());
            refreshVisibleExpenses();
        }
    }

    /**
     * Replaces current expenses with the ones provided, preserving active filters and sorting.
     * @param newExpenses - the new expenses
     */
    public void updateExpenseList(final List<Expense> newExpenses) {
        allExpenses.clear();
        allExpenses.addAll(newExpenses);

        mapExpenseIdToExpense.clear();
        for (Expense expense : newExpenses) {
            mapExpenseIdToExpense.put(expense.getId(), expense);
        }

        refreshVisibleExpenses();
        notifyDataSetChanged();
    }

    public List<Expense> getExpenses(final boolean includeUnfiltered) {
        return new ArrayList<>(includeUnfiltered ? allExpenses : expenses);
    }

    public Map<ExpenseType, Integer> getExpenseTypes() {
        final Map<ExpenseType, Integer> mapExpenseTypesToCount = new HashMap<>();

        for (Expense expense : expenses) {
            final ExpenseType expenseType = expense.getExpenseType();
            mapExpenseTypesToCount.merge(expenseType, 1, Integer::sum);
        }

        return mapExpenseTypesToCount;
    }

    public void deleteExpense(final Expense expense) {
        deleteExpense(expense.getId());
    }

    public void deleteExpense(final Long expenseId) {
        requireNonNull(expenseId);

        allExpenses.removeIf(expenseItem -> Objects.equals(expenseItem.getId(), expenseId));
        mapExpenseIdToExpense.remove(expenseId);
        refreshVisibleExpenses();
    }

    public void sortExpenses(final Comparator<Expense> comparator) {
        this.comparator = requireNonNull(comparator);
        refreshVisibleExpenses();
        notifyDataSetChanged();
    }

    public boolean isFiltered() {
        return filters != null;
    }

    public void filterBy(final Filters filters) {
        this.filters = requireNonNull(filters);
        refreshVisibleExpenses();
        notifyDataSetChanged();
    }

    private void refreshVisibleExpenses() {
        final List<Expense> visibleExpenses = allExpenses.stream()
                                                         .filter(this::isVisible)
                                                         .toList();
        expenses.clear();
        expenses.addAll(visibleExpenses);
        expenses.sort(comparator);
    }

    private boolean isVisible(final Expense expense) {
        return !isFiltered()
                || filters.expenseTypes().contains(expense.getExpenseType())
                || matchesComment(expense.getComment());
    }

    private boolean matchesComment(final String comment) {
        return filters.comments()
                      .stream()
                      .anyMatch(comment::contains);
    }

    public Set<ExpenseType> expenseTypeSet() {
        Set<ExpenseType> expenseTypeSet = new HashSet<>();

        for (Expense expense : allExpenses) {
            expenseTypeSet.add(expense.getExpenseType());
        }

        return expenseTypeSet;
    }

    public void clearFilters() {
        filters = null;
        refreshVisibleExpenses();
        notifyDataSetChanged();
    }

}
