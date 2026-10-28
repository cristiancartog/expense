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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TaskRunner implements DefaultLifecycleObserver, AutoCloseable {

    private static final Consumer<Object> NO_OP_CONSUMER = ignored -> { };

    @FunctionalInterface
    public interface ProgressTask<P> {
        void run(Consumer<P> progressPublisher) throws Exception;
    }

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
    public <T> void submit(final Supplier<T> backgroundOperation,
                           final Consumer<T> resultConsumer) {
        submit(
                backgroundOperation,
                resultConsumer,
                NO_OP_CONSUMER
        );
    }

    @MainThread
    public <T> void submit(final Supplier<T> backgroundOperation,
                           final Consumer<T> resultConsumer,
                           final Consumer<? super Exception> errorConsumer) {
        final AtomicReference<T> result = new AtomicReference<>();
        submit(
                progressPublisher -> result.set(backgroundOperation.get()),
                NO_OP_CONSUMER,
                errorConsumer,
                () -> resultConsumer.accept(result.get())
        );
    }

    @MainThread
    public <P> void submit(final ProgressTask<P> backgroundOperation,
                           final Consumer<? super P> progressConsumer,
                           final Consumer<? super Exception> errorConsumer,
                           final Runnable completionCallback) {
        if (closed) {
            throw new IllegalStateException("Cannot submit work to a closed TaskRunner");
        }

        final int taskGeneration = ++generation;
        if (currentTask != null) {
            currentTask.cancel(true);
        }

        currentTask = executor.submit(() -> {
            final Consumer<P> progressPublisher = progress -> postIfCurrent(
                    taskGeneration, () -> progressConsumer.accept(progress));
            try {
                backgroundOperation.run(progressPublisher);
            } catch (Exception error) {
                if (!Thread.currentThread().isInterrupted()) {
                    postIfCurrent(taskGeneration, () -> errorConsumer.accept(error));
                }
                return;
            }

            if (!Thread.currentThread().isInterrupted()) {
                postIfCurrent(taskGeneration, completionCallback);
            }
        });
    }

    private void postIfCurrent(final int taskGeneration, final Runnable callback) {
        mainThreadHandler.post(() -> {
            if (!closed && taskGeneration == generation) {
                callback.run();
            }
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
