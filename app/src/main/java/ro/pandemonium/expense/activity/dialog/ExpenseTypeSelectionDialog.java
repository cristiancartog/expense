package ro.pandemonium.expense.activity.dialog;

import static ro.pandemonium.expense.util.StringUtils.isNotEmpty;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.pandemonium.expense.R;
import ro.pandemonium.expense.model.ExpenseType;
import ro.pandemonium.expense.model.Filters;
import ro.pandemonium.expense.view.adapter.ExpenseTypeListAdapter;

public class ExpenseTypeSelectionDialog extends Dialog {

    private static final LinearLayout.LayoutParams COMMENT_LAYOUT_PARAMS =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

    private ExpenseTypeListAdapter adapter;
    private ListView expenseTypeList;
    private LinearLayout commentsSection;
    private EditText commentField;
    private Callback callback;
    private final Set<String> comments;

    public interface Callback {
        void expenseTypesSelected(Filters filters);
    }

    public ExpenseTypeSelectionDialog(final Context context) {
        super(context);

        setTitle(R.string.filterExpenseDialogTitle);
        comments = new HashSet<>();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expense_type_selection);

        adapter = new ExpenseTypeListAdapter();
        expenseTypeList = findViewById(R.id.expenseTypeSelectionDialogListView);
        expenseTypeList.setAdapter(adapter);
        expenseTypeList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        final Button selectAllButton = findViewById(R.id.expenseTypeSelectionDialogSelectAllButton);
        final Button selectNoneButton = findViewById(R.id.expenseTypeSelectionDialogSelectNoneButton);
        final Button invertSelectionButton = findViewById(R.id.expenseTypeSelectionDialogInvertSelectionButton);
        final Button singlesSelectionButton = findViewById(R.id.expenseTypeSelectionDialogSinglesButton);
        final Button okButton = findViewById(R.id.expenseTypeSelectionDialogOkButton);
        final Button cancelButton = findViewById(R.id.expenseTypeSelectionDialogCancelButton);
        final Button addCommentButton = findViewById(R.id.expenseTypeSelectionDialogAddCommentButton);
        commentField = findViewById(R.id.expenseTypeSelectionDialogCommentEditText);
        commentsSection = findViewById(R.id.expenseTypeSelectionDialogComments);

        selectAllButton.setOnClickListener(this::selectAllExpenseTypes);
        selectNoneButton.setOnClickListener(this::selectNoExpenseTypes);
        invertSelectionButton.setOnClickListener(this::invertSelection);
        singlesSelectionButton.setOnClickListener(this::selectSinglesExpenseTypes);
        addCommentButton.setOnClickListener(this::addComment);
        okButton.setOnClickListener(this::okClicked);
        cancelButton.setOnClickListener(this::cancelClicked);
    }

    public void show(final Filters filters, final Collection<ExpenseType> availableExpenseTypes, final Callback callback) {
        show();
        this.callback = callback;
        adapter.setAvailableExpenseTypes(availableExpenseTypes);
        adapter.notifyDataSetChanged();

        if (filters != null) {
            selectExpenseTypes(filters.getExpenseTypes());
            setComments(filters.getComments());
        } else {
            selectNone();
        }

        commentField.setText("");
    }

    private Filters getSelectedExpenseTypes() {
        final List<ExpenseType> expenseTypes = new ArrayList<>();
        final SparseBooleanArray selectedPositions = expenseTypeList.getCheckedItemPositions();

        for (int i = 0; i < selectedPositions.size(); i++) {
            final int key = selectedPositions.keyAt(i);
            if (selectedPositions.get(key)) {
                expenseTypes.add((ExpenseType) adapter.getItem(key));
            }
        }

        return new Filters(expenseTypes, new HashSet<>(comments));
    }

    private void selectAllExpenseTypes(final View view) {
        final int adapterCount = adapter.getCount();
        for (int i = 0; i < adapterCount; i++) {
            expenseTypeList.setItemChecked(i, true);
        }
    }

    private void selectNoExpenseTypes(final View view) {
        selectNone();
    }

    private void invertSelection(final View view) {
        final int adapterCount = adapter.getCount();
        final SparseBooleanArray checkedItemPositions = expenseTypeList.getCheckedItemPositions();
        for (int i = 0; i < adapterCount; i++) {
            final boolean checked = checkedItemPositions.get(i, false);
            expenseTypeList.setItemChecked(i, !checked);
        }
    }

    private void selectSinglesExpenseTypes(final View view) {
        final int adapterCount = adapter.getCount();
        for (int i = 0; i < adapterCount; i++) {
            final ExpenseType expenseType = (ExpenseType) adapter.getItem(i);
            expenseTypeList.setItemChecked(i, expenseType.getMaxOccurrences() == 1);
        }
    }

    private void addComment(final View view) {
        final String comment = commentField.getText().toString().trim();
        if (isNotEmpty(comment)) {
            addCommentsLineItem(comment);
        }
        commentField.setText("");
    }

    private void okClicked(final View view) {
        final Filters selectedExpenseTypes = getSelectedExpenseTypes();
        if (selectedExpenseTypes.isSelectionEmpty()) {
            Toast.makeText(getContext(), R.string.filterExpenseDialogEmptySelection, Toast.LENGTH_SHORT).show();
        } else {
            hide();
            if (callback != null) {
                callback.expenseTypesSelected(selectedExpenseTypes);
            }
        }
    }

    private void cancelClicked(final View view) {
        hide();
    }

    private void selectExpenseTypes(final List<ExpenseType> expenseTypes) {
        if (expenseTypes == null) {
            selectNone();
        } else {
            final int adapterCount = adapter.getCount();
            for (int i = 0; i < adapterCount; i++) {
                final ExpenseType expenseType = (ExpenseType) adapter.getItem(i);
                expenseTypeList.setItemChecked(i, expenseTypes.contains(expenseType));
            }
        }
    }

    private void setComments(Set<String> comments) {
        this.comments.clear();
        commentsSection.removeAllViews();
        for (String comment : comments) {
            comments.add(comment);
            addCommentsLineItem(comment);
        }
    }

    private void selectNone() {
        for (int i = 0; i < adapter.getCount(); i++) {
            expenseTypeList.setItemChecked(i, false);
        }
    }

    private void addCommentsLineItem(String comment) {
        if (comments.add(comment)) {
            final LinearLayout commentLineItem = new LinearLayout(this.getContext());
            final TextView textView = new TextView(commentLineItem.getContext());
            textView.setLayoutParams(COMMENT_LAYOUT_PARAMS);
            textView.setText(comment);

            final Button removeCommentButton = new Button(commentLineItem.getContext());
            removeCommentButton.setText(R.string.filterExpenseDialogRemoveComment);
            removeCommentButton.setOnClickListener(view -> {
                commentsSection.removeView(commentLineItem);
                comments.remove(textView.getText().toString());
            });

            commentLineItem.addView(textView);
            commentLineItem.addView(removeCommentButton);

            commentsSection.addView(commentLineItem);
        }
    }
}
