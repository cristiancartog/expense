package ro.pandemonium.expense.activity;

import static android.app.Activity.RESULT_CANCELED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ro.pandemonium.expense.Constants.DATE_FORMAT_PATTERN_MONTH;
import static ro.pandemonium.expense.model.ExpenseType.FOOD;
import static ro.pandemonium.expense.model.ExpenseType.PETROL;
import static ro.pandemonium.expense.model.ExpenseType.SPECIAL;
import static ro.pandemonium.expense.model.Filters.EMPTY_FILTERS;

import android.app.Instrumentation;
import android.content.Context;
import android.os.SystemClock;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.activity.chart.ExpenseHistoryChartActivity;
import ro.pandemonium.expense.activity.dialog.ExpenseTypeSelectionDialog;
import ro.pandemonium.expense.db.ExpenseDatabase;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.service.ExpenseMainService;

@RunWith(AndroidJUnit4.class)
public class ExpenseTypeSelectionDialogTest {

    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private ExpenseDatabase database;
    private ActivityScenario<ExpenseMainActivity> scenario;
    private Instrumentation.ActivityMonitor searchMonitor;
    private Instrumentation.ActivityMonitor historyMonitor;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase.class)
                       .allowMainThreadQueries()
                       .build();
        scenario = ActivityScenario.launch(ExpenseMainActivity.class);
        final YearMonth month = YearMonth.of(2000, 1);
        scenario.onActivity(activity -> {
            activity.mainService = new ExpenseMainService(database.expenseDao(), context);
            activity.mainService.initialize();
            activity.onDateSet(null, month.getYear(), month.getMonthValue() - 1, 1);
        });
        final String monthLabel = month.format(DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN_MONTH, Locale.getDefault()));
        final AtomicBoolean loaded = new AtomicBoolean();
        final long deadline = SystemClock.uptimeMillis() + 5_000;
        while (!loaded.get() && SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                final TextView monthButton = activity.findViewById(R.id.expenseListChangeMonthButton);
                loaded.set(monthLabel.contentEquals(monthButton.getText()));
            });
            if (!loaded.get()) {
                SystemClock.sleep(20);
            }
        }
        assertTrue("Selected month did not load", loaded.get());
        final Instrumentation.ActivityResult canceled = new Instrumentation.ActivityResult(RESULT_CANCELED, null);
        searchMonitor = instrumentation.addMonitor(ExpenseSearchResultActivity.class.getName(), canceled, true);
        historyMonitor = instrumentation.addMonitor(ExpenseHistoryChartActivity.class.getName(), canceled, true);
    }

    @After
    public void tearDown() {
        if (searchMonitor != null) {
            instrumentation.removeMonitor(searchMonitor);
        }
        if (historyMonitor != null) {
            instrumentation.removeMonitor(historyMonitor);
        }
        if (scenario != null) {
            scenario.close();
        }
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void switchingBetweenLocalSearchAndHistoryUsesOnlyTheirOwnFilters() {
        scenario.onActivity(activity -> {
            final ExpenseTypeSelectionDialog dialog = activity.expenseTypeSelectionDialog;
            activity.filterList(null);
            assertDraft(dialog, List.of(), Set.of());
            addComment(dialog, "local");
            click(dialog, R.id.expenseTypeSelectionDialogOkButton);

            activity.dbSearchRequested(null);
            assertDraft(dialog, List.of(), Set.of());
            addComment(dialog, "search");
            selectType(dialog, SPECIAL);
            click(dialog, R.id.expenseTypeSelectionDialogOkButton);
            assertEquals(1, searchMonitor.getHits());

            activity.expenseHistoryRequested(null);
            assertDraft(dialog, List.of(), Set.of());
            selectType(dialog, PETROL);
            click(dialog, R.id.expenseTypeSelectionDialogOkButton);
            assertEquals(1, historyMonitor.getHits());

            activity.dbSearchRequested(null);
            assertDraft(dialog, List.of(SPECIAL), Set.of("search"));
            removeComment(dialog, "search");
            addComment(dialog, "unconfirmed replacement");
            click(dialog, R.id.expenseTypeSelectionDialogCancelButton);

            activity.filterList(null);
            assertDraft(dialog, List.of(), Set.of("local"));
            removeComment(dialog, "local");
            click(dialog, R.id.expenseTypeSelectionDialogCancelButton);

            activity.expenseHistoryRequested(null);
            assertDraft(dialog, List.of(), Set.of());
            click(dialog, R.id.expenseTypeSelectionDialogCancelButton);

            activity.dbSearchRequested(null);
            assertDraft(dialog, List.of(SPECIAL), Set.of("search"));
            click(dialog, R.id.expenseTypeSelectionDialogCancelButton);

            activity.filterList(null);
            assertDraft(dialog, List.of(), Set.of("local"));
        });
    }

    @Test
    public void cancelAndReopenDiscardUnconfirmedCommentsInEveryUse() {
        scenario.onActivity(activity -> {
            final ExpenseTypeSelectionDialog dialog = activity.expenseTypeSelectionDialog;
            for (String use : List.of("local", "search", "history")) {
                open(activity, use);
                addComment(dialog, "draft");
                click(dialog, R.id.expenseTypeSelectionDialogSelectAllButton);
                commentField(dialog).setText("unsubmitted");
                click(dialog, R.id.expenseTypeSelectionDialogCancelButton);
                assertFalse(dialog.isShowing());

                open(activity, use);
                assertDraft(dialog, List.of(), Set.of());
                addComment(dialog, "another draft");
                // Back and outside-tap cancellation both use Dialog.cancel().
                dialog.cancel();
                open(activity, use);
                assertDraft(dialog, List.of(), Set.of());
                click(dialog, R.id.expenseTypeSelectionDialogCancelButton);
            }
            assertEquals(0, searchMonitor.getHits());
            assertEquals(0, historyMonitor.getHits());
        });
    }

    private void open(final ExpenseMainActivity activity, final String use) {
        switch (use) {
            case "local" -> activity.filterList(null);
            case "search" -> activity.dbSearchRequested(null);
            case "history" -> activity.expenseHistoryRequested(null);
            default -> throw new IllegalArgumentException(use);
        }
    }

    @Test
    public void draftEditsDoNotMutateSuppliedFiltersOrPreviouslyAcceptedResults() {
        scenario.onActivity(activity -> {
            final ExpenseTypeSelectionDialog dialog = activity.expenseTypeSelectionDialog;
            final Filters supplied = new Filters(new ArrayList<>(List.of(FOOD)), new HashSet<>(Set.of("saved")));
            final AtomicReference<Filters> accepted = new AtomicReference<>();
            dialog.show(supplied, ExpenseType.orderedExpenseTypes(), accepted::set);
            removeComment(dialog, "saved");
            addComment(dialog, "draft");
            click(dialog, R.id.expenseTypeSelectionDialogSelectAllButton);
            assertEquals(new Filters(List.of(FOOD), Set.of("saved")), supplied);
            assertNull(accepted.get());
            click(dialog, R.id.expenseTypeSelectionDialogCancelButton);
            assertNull(accepted.get());

            dialog.show(supplied.copy(), ExpenseType.orderedExpenseTypes(), accepted::set);
            assertDraft(dialog, List.of(FOOD), Set.of("saved"));
            click(dialog, R.id.expenseTypeSelectionDialogOkButton);
            final Filters firstResult = accepted.get();
            assertEquals(supplied, firstResult);
            assertFalse(dialog.isShowing());

            dialog.show(firstResult, ExpenseType.orderedExpenseTypes(), accepted::set);
            removeComment(dialog, "saved");
            addComment(dialog, "replacement");
            click(dialog, R.id.expenseTypeSelectionDialogSelectNoneButton);
            assertEquals(supplied, firstResult);
            assertEquals(new Filters(List.of(FOOD), Set.of("saved")), supplied);
            click(dialog, R.id.expenseTypeSelectionDialogOkButton);
            assertEquals(new Filters(List.of(), Set.of("replacement")), accepted.get());
            assertEquals(supplied, firstResult);
        });
    }

    @Test
    public void selectNoneClearsCategoriesAndAcceptsOnlyTheVisibleComments() {
        scenario.onActivity(activity -> {
            final ExpenseTypeSelectionDialog dialog = activity.expenseTypeSelectionDialog;
            final AtomicReference<Filters> accepted = new AtomicReference<>();
            dialog.show(new Filters(List.of(FOOD, SPECIAL), Set.of("kept")), ExpenseType.orderedExpenseTypes(), accepted::set);
            click(dialog, R.id.expenseTypeSelectionDialogSelectNoneButton);
            assertDraft(dialog, List.of(), Set.of("kept"));
            click(dialog, R.id.expenseTypeSelectionDialogOkButton);
            assertEquals(new Filters(List.of(), Set.of("kept")), accepted.get());
            assertFalse(dialog.isShowing());
        });
    }

    @Test
    public void reopeningWithFewerCategoriesClearsOldChecksAndRejectsAnEmptySelection() {
        scenario.onActivity(activity -> {
            final ExpenseTypeSelectionDialog dialog = activity.expenseTypeSelectionDialog;
            final AtomicReference<Filters> accepted = new AtomicReference<>();
            dialog.show(new Filters(List.of(SPECIAL), Set.of("old")), ExpenseType.orderedExpenseTypes(), accepted::set);
            click(dialog, R.id.expenseTypeSelectionDialogCancelButton);

            dialog.show(EMPTY_FILTERS, List.of(FOOD), accepted::set);
            assertDraft(dialog, List.of(), Set.of());
            click(dialog, R.id.expenseTypeSelectionDialogSelectAllButton);
            click(dialog, R.id.expenseTypeSelectionDialogSelectNoneButton);
            click(dialog, R.id.expenseTypeSelectionDialogOkButton);
            assertTrue(dialog.isShowing());
            assertNull(accepted.get());
            selectType(dialog, FOOD);
            click(dialog, R.id.expenseTypeSelectionDialogOkButton);
            assertEquals(new Filters(List.of(FOOD), Set.of()), accepted.get());
        });
    }

    private void assertDraft(final ExpenseTypeSelectionDialog dialog, final List<ExpenseType> types, final Set<String> comments) {
        final ListView list = dialog.findViewById(R.id.expenseTypeSelectionDialogListView);
        assertEquals(types.size(), list.getCheckedItemCount());
        for (int i = 0; i < list.getCount(); i++) {
            assertEquals(types.contains(list.getItemAtPosition(i)), list.isItemChecked(i));
        }
        final LinearLayout section = dialog.findViewById(R.id.expenseTypeSelectionDialogComments);
        final Set<String> visibleComments = new HashSet<>();
        int commentCount = 0;
        for (int i = 0; i < section.getChildCount(); i++) {
            if (section.getChildAt(i) instanceof LinearLayout row) {
                visibleComments.add(((TextView) row.getChildAt(0)).getText().toString());
                commentCount++;
            }
        }
        assertEquals(comments.size(), commentCount);
        assertEquals(comments, visibleComments);
        assertEquals("", commentField(dialog).getText().toString());
    }

    private void selectType(final ExpenseTypeSelectionDialog dialog, final ExpenseType type) {
        final ListView list = dialog.findViewById(R.id.expenseTypeSelectionDialogListView);
        for (int i = 0; i < list.getCount(); i++) {
            if (list.getItemAtPosition(i) == type) {
                list.setItemChecked(i, true);
                return;
            }
        }
        throw new AssertionError("Category is unavailable: " + type);
    }

    private void addComment(final ExpenseTypeSelectionDialog dialog, final String comment) {
        commentField(dialog).setText(comment);
        click(dialog, R.id.expenseTypeSelectionDialogAddCommentButton);
    }

    private void removeComment(final ExpenseTypeSelectionDialog dialog, final String comment) {
        final LinearLayout section = dialog.findViewById(R.id.expenseTypeSelectionDialogComments);
        for (int i = 0; i < section.getChildCount(); i++) {
            final LinearLayout row = (LinearLayout) section.getChildAt(i);
            if (comment.contentEquals(((TextView) row.getChildAt(0)).getText())) {
                row.getChildAt(1).performClick();
                return;
            }
        }
        throw new AssertionError("Comment is not visible: " + comment);
    }

    private EditText commentField(final ExpenseTypeSelectionDialog dialog) {
        return dialog.findViewById(R.id.expenseTypeSelectionDialogCommentEditText);
    }

    private void click(final ExpenseTypeSelectionDialog dialog, final int id) {
        dialog.findViewById(id).performClick();
    }
}
