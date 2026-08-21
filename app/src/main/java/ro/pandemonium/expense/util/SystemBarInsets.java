package ro.pandemonium.expense.util;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.navigation.NavigationView;

public final class SystemBarInsets {

    private SystemBarInsets() {
    }

    public static void applySystemBarInsets(final NavigationView navigationView) {
        final int initialPaddingLeft = navigationView.getPaddingLeft();
        final int initialPaddingTop = navigationView.getPaddingTop();
        final int initialPaddingRight = navigationView.getPaddingRight();
        final int initialPaddingBottom = navigationView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(navigationView, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    initialPaddingLeft + systemBars.left,
                    initialPaddingTop + systemBars.top,
                    initialPaddingRight + systemBars.right,
                    initialPaddingBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(navigationView);
    }

    public static void applyBottomInset(final Activity activity) {
        final View content = activity.findViewById(android.R.id.content);
        final int initialPaddingLeft = content.getPaddingLeft();
        final int initialPaddingTop = content.getPaddingTop();
        final int initialPaddingRight = content.getPaddingRight();
        final int initialPaddingBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            final Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(
                    initialPaddingLeft,
                    initialPaddingTop,
                    initialPaddingRight,
                    initialPaddingBottom + navigationBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
