package ro.pandemonium.expense.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TaskRunner implements DefaultLifecycleObserver, AutoCloseable {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private Future<?> currentTask;
    private int generation;
    private boolean closed;

    @MainThread
    public TaskRunner(final LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    @MainThread
    public <T> void submit(final Supplier<T> backgroundOperation, final Consumer<T> resultConsumer) {
        if (closed) {
            throw new IllegalStateException("Cannot submit work to a closed TaskRunner");
        }

        final int taskGeneration = ++generation;
        if (currentTask != null) {
            currentTask.cancel(true);
        }

        currentTask = executor.submit(() -> {
            final T result = backgroundOperation.get();
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            mainThreadHandler.post(() -> {
                if (!closed && taskGeneration == generation) {
                    resultConsumer.accept(result);
                }
            });
        });
    }

    @Override
    @MainThread
    public void onDestroy(@NonNull final LifecycleOwner owner) {
        close();
    }

    @Override
    @MainThread
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        generation++;
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
        mainThreadHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }
}
