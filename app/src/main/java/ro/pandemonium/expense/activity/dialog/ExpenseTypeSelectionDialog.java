package ro.pandemonium.expense.activity.dialog;

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

public class ExpenseTypeSelectionDialog extends Dialog implements View.OnClickListener {

    public static final LinearLayout.LayoutParams COMMENT_LAYOUT_PARAMS =
            new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);

    private ExpenseTypeListAdapter adapter;
    private ListView expenseTypeList;
    private LinearLayout commentsSection;
    private EditText commentField;
    private Callback callback;
    private Set<String> comments;

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
        expenseTypeList = (ListView) findViewById(R.id.expenseTypeSelectionDialogListView);
        expenseTypeList.setAdapter(adapter);
        expenseTypeList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        final Button selectAllButton = (Button) findViewById(R.id.expenseTypeSelectionDialogSelectAllButton);
        final Button selectNoneButton = (Button) findViewById(R.id.expenseTypeSelectionDialogSelectNoneButton);
        final Button invertSelectionButton = (Button) findViewById(R.id.expenseTypeSelectionDialogInverSelectionButton);
        final Button singlesSelectionButton = (Button) findViewById(R.id.expenseTypeSelectionDialogSinglesButton);
        final Button okButton = (Button) findViewById(R.id.expenseTypeSelectionDialogOkButton);
        final Button cancelButton = (Button) findViewById(R.id.expenseTypeSelectionDialogCancelButton);
        final Button addCommentButton = (Button) findViewById(R.id.expenseTypeSelectionDialogAddCommentButton);
        commentField = (EditText) findViewById(R.id.expenseTypeSelectionDialogCommentEditText);
        commentsSection = (LinearLayout) findViewById(R.id.expenseTypeSelectionDialogComments);

        selectAllButton.setOnClickListener(this);
        selectNoneButton.setOnClickListener(this);
        invertSelectionButton.setOnClickListener(this);
        singlesSelectionButton.setOnClickListener(this);
        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        addCommentButton.setOnClickListener(this);
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

    @Override
    public void onClick(final View view) {
        final int adapterCount = adapter.getCount();
        switch (view.getId()) {
            case R.id.expenseTypeSelectionDialogSelectAllButton:
                for (int i = 0; i < adapterCount; i++) {
                    expenseTypeList.setItemChecked(i, true);
                }
                break;
            case R.id.expenseTypeSelectionDialogSelectNoneButton:
                selectNone();
                break;
            case R.id.expenseTypeSelectionDialogSinglesButton:
                for (int i = 0; i < adapterCount; i++) {
                    final ExpenseType expenseType = (ExpenseType) adapter.getItem(i);
                    expenseTypeList.setItemChecked(i, expenseType.getMaxOccurences() == 1);
                }
                break;
            case R.id.expenseTypeSelectionDialogInverSelectionButton:
                final SparseBooleanArray checkedItemPositions = expenseTypeList.getCheckedItemPositions();
                for (int i = 0; i < adapterCount; i++) {
                    final boolean checked = checkedItemPositions.get(i, false);
                    expenseTypeList.setItemChecked(i, !checked);
                }
                break;
            case R.id.expenseTypeSelectionDialogAddCommentButton:
                final String comment = commentField.getText().toString().trim();
                if (!"".equals(comment)) {
                    addCommentsLineItem(commentField.getText().toString());
                }
                commentField.setText("");
                break;
            case R.id.expenseTypeSelectionDialogOkButton:
                final Filters selectedExpenseTypes = getSelectedExpenseTypes();
                if (selectedExpenseTypes.isSelectionEmpty()) {
                    Toast.makeText(getContext(), R.string.filterExpenseDialogEmptySelection, Toast.LENGTH_SHORT).show();
                } else {
                    hide();
                    if (callback != null) {
                        callback.expenseTypesSelected(selectedExpenseTypes);
                    }
                }
                break;
            case R.id.expenseTypeSelectionDialogCancelButton:
                hide();
                break;
        }
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
            removeCommentButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view) {
                    commentsSection.removeView(commentLineItem);
                    comments.remove(textView.getText().toString());
                }
            });

            commentLineItem.addView(textView);
            commentLineItem.addView(removeCommentButton);

            commentsSection.addView(commentLineItem);
        }
    }
}
