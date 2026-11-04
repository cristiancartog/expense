package ro.pandemonium.expense.util;

import static android.widget.Toast.LENGTH_LONG;
import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public final class LoadErrorUtil {

    private LoadErrorUtil() {
    }

    public static void show(final Context context,
                            @StringRes final int messageResource,
                            final Exception error) {
        requireNonNull(context);
        requireNonNull(error);

        final String detail = error.getMessage() != null ? error.getMessage() : error.toString();
        Toast.makeText(context, context.getString(messageResource, detail), LENGTH_LONG)
             .show();
    }
}
