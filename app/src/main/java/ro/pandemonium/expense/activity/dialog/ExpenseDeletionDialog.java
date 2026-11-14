package ro.pandemonium.expense.activity.dialog;

import static java.util.Objects.requireNonNull;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import ro.pandemonium.expense.R;

public class ExpenseDeletionDialog extends AlertDialog {

    private Runnable callback;

    public ExpenseDeletionDialog(final Context context) {
        super(context);

        setIcon(android.R.drawable.ic_dialog_alert);
        setTitle(R.string.removeExpenseDialogTitle);
        setMessage(context.getString(R.string.removeExpenseDialogMessage));
        setButton(
                BUTTON_POSITIVE,
                context.getString(R.string.removeExpenseDialogYes),
                (dialog, which) -> callback.run()
        );
        setButton(
                BUTTON_NEGATIVE,
                context.getString(R.string.removeExpenseDialogNo),
                (DialogInterface.OnClickListener) null
        );
    }

    public void show(final Runnable callback) {
        this.callback = requireNonNull(callback);
        show();
    }
}
