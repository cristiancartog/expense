package ro.pandemonium.expense.util;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class SystemBarInsets {

    private SystemBarInsets() {
    }

    public static void keepContentInsideSystemBars(final Activity activity) {
        keepContentInsideSystemBars(activity.findViewById(android.R.id.content));
    }

    public static void keepContentInsideSystemBars(final View content) {
        final int initialPaddingLeft = content.getPaddingLeft();
        final int initialPaddingTop = content.getPaddingTop();
        final int initialPaddingRight = content.getPaddingRight();
        final int initialPaddingBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            final Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    initialPaddingLeft + systemBars.left,
                    initialPaddingTop + systemBars.top,
                    initialPaddingRight + systemBars.right,
                    initialPaddingBottom + systemBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
