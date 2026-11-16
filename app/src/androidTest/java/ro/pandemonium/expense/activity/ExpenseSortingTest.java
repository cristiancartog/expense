package ro.pandemonium.expense.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static ro.pandemonium.expense.Constants.INTENT_FILTERS;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.OTHER;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;
import static ro.pandemonium.expense.model.ExpenseType.TAXES;

import android.content.Intent;
import android.view.Menu;
import android.widget.PopupMenu;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.Expense;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.view.adapter.ExpenseListAdapter;

@RunWith(Parameterized.class)
public class ExpenseSortingTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> sorts() {
        return List.of(
                new Object[]{"date", R.id.main_menu_order_by_date},
                new Object[]{"type", R.id.main_menu_order_by_type},
                new Object[]{"value", R.id.main_menu_order_by_value}
        );
    }

    @Parameterized.Parameter
    public String sort;
    @Parameterized.Parameter(1)
    public int menuItemId;

    private ActivityScenario<ExpenseSearchResultActivity> scenario;

    @Before
    public void setUp() {
        final Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ExpenseSearchResultActivity.class);
        intent.putExtra(INTENT_FILTERS, new Filters(List.of(), Set.of()));
        scenario = ActivityScenario.launch(intent);
        scenario.onActivity(activity -> activity.expenseListAdapter.updateExpenseList(List.of(
                expense(1, PETROL, 20, "10.00", "match"),
                expense(2, FOOD, 10, "30.00", "match"),
                expense(3, OTHER, 15, "20.00", "hidden")
        )));
    }

    private Expense expense(final long id, final ExpenseType type, final int day, final String value, final String comment) {
        final Expense expense = new Expense();
        expense.setId(id);
        expense.setExpenseType(type);
        expense.setDate(LocalDate.of(2000, 1, day));
        expense.setValue(new BigDecimal(value));
        expense.setComment(comment);
        return expense;
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void insertsAndSortKeyEditsKeepTheSelectedOrder() {
        scenario.onActivity(activity -> {
            selectSort(activity);
            final ExpenseListAdapter adapter = activity.expenseListAdapter;
            assertIds(adapter, List.of(2L, 3L, 1L), List.of(3L, 2L, 1L), List.of(1L, 3L, 2L));

            adapter.addExpense(expense(4, TAXES, 5, "5.00", "match"));
            assertIds(adapter, List.of(4L, 2L, 3L, 1L), List.of(3L, 2L, 1L, 4L), List.of(4L, 1L, 3L, 2L));

            adapter.updateExpense(expense(1, SPECIAL, 1, "40.00", "match"));
            assertIds(adapter, List.of(1L, 4L, 2L, 3L), List.of(3L, 2L, 4L, 1L), List.of(4L, 3L, 2L, 1L));
            adapter.deleteExpense(4L);
            assertIds(adapter, List.of(1L, 2L, 3L), List.of(3L, 2L, 1L), List.of(3L, 2L, 1L));
        });
    }

    private void selectSort(final ExpenseSearchResultActivity activity) {
        final Menu menu = new PopupMenu(activity, activity.expenseList).getMenu();
        activity.onCreateOptionsMenu(menu);
        activity.sortExpenses(menu.findItem(menuItemId));
    }

    private void assertIds(final ExpenseListAdapter adapter, final List<Long> dateIds, final List<Long> typeIds, final List<Long> valueIds) {
        final List<Long> expected = switch (sort) {
            case "date" -> dateIds;
            case "type" -> typeIds;
            case "value" -> valueIds;
            default -> throw new IllegalArgumentException(sort);
        };
        assertEquals(expected, adapter.getExpenses(false).stream()
                                     .map(Expense::getId)
                                     .toList());
        assertEquals(expected.size(), adapter.getCount());
        for (int index = 0; index < expected.size(); index++) {
            assertEquals(expected.get(index), ((Expense) adapter.getItem(index)).getId());
        }
    }

    @Test
    public void sortingWhileFilteredSurvivesHiddenEditsAndRepeatedFilterCycles() {
        scenario.onActivity(activity -> {
            final ExpenseListAdapter adapter = activity.expenseListAdapter;
            adapter.sortExpenses(java.util.Comparator.comparing(Expense::getId).reversed());
            final Filters filter = new Filters(List.of(), Set.of("match"));
            adapter.filterBy(filter);
            selectSort(activity);
            assertIds(adapter, List.of(2L, 1L), List.of(2L, 1L), List.of(1L, 2L));

            adapter.addExpense(expense(4, TAXES, 5, "5.00", "match"));
            adapter.addExpense(expense(5, FOOD, 25, "50.00", "hidden"));
            assertIds(adapter, List.of(4L, 2L, 1L), List.of(2L, 1L, 4L), List.of(4L, 1L, 2L));
            adapter.updateExpense(expense(3, OTHER, 1, "1.00", "hidden"));
            adapter.updateExpense(expense(1, SPECIAL, 30, "60.00", "hidden"));
            assertIds(adapter, List.of(4L, 2L), List.of(2L, 4L), List.of(4L, 2L));
            adapter.updateExpense(expense(5, FOOD, 25, "50.00", "match"));
            assertIds(adapter, List.of(4L, 2L, 5L), List.of(2L, 5L, 4L), List.of(4L, 2L, 5L));

            for (int cycle = 0; cycle < 2; cycle++) {
                adapter.clearFilters();
                assertFalse(adapter.isFiltered());
                assertIds(adapter, List.of(3L, 4L, 2L, 5L, 1L), List.of(3L, 2L, 5L, 4L, 1L), List.of(3L, 4L, 2L, 5L, 1L));
                adapter.filterBy(filter);
                assertIds(adapter, List.of(4L, 2L, 5L), List.of(2L, 5L, 4L), List.of(4L, 2L, 5L));
            }
        });
    }

    @Test
    public void replacingExpensesPreservesTheFilterAndSelectedSort() {
        scenario.onActivity(activity -> {
            final ExpenseListAdapter adapter = activity.expenseListAdapter;
            selectSort(activity);
            adapter.filterBy(new Filters(List.of(FOOD, PETROL), Set.of()));
            adapter.updateExpenseList(List.of(
                    expense(1, PETROL, 1, "40.00", "match"),
                    expense(2, FOOD, 10, "30.00", "match"),
                    expense(3, OTHER, 15, "20.00", "hidden")
            ));
            assertIds(adapter, List.of(1L, 2L), List.of(2L, 1L), List.of(2L, 1L));
            adapter.clearFilters();
            assertIds(adapter, List.of(1L, 2L, 3L), List.of(3L, 2L, 1L), List.of(3L, 2L, 1L));
        });
    }

    @Test
    public void typeSortMatchesTheCategorySelectionOrder() {
        scenario.onActivity(activity -> {
            final List<ExpenseType> types = new ArrayList<>(ExpenseType.orderedExpenseTypes());
            Collections.reverse(types);
            activity.expenseListAdapter.updateExpenseList(types.stream()
                                                             .map(type -> expense(type.getDbId(), type, 1, "1.00", ""))
                                                             .toList());
            final Menu menu = new PopupMenu(activity, activity.expenseList).getMenu();
            activity.onCreateOptionsMenu(menu);
            activity.sortExpenses(menu.findItem(R.id.main_menu_order_by_type));
            assertEquals(ExpenseType.orderedExpenseTypes(), activity.expenseListAdapter.getExpenses(false).stream()
                                                                                     .map(Expense::getExpenseType)
                                                                                     .toList());
        });
    }
}
