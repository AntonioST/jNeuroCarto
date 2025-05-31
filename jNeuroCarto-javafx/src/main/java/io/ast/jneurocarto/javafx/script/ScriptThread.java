package io.ast.jneurocarto.javafx.script;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import javafx.application.Platform;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.javafx.app.BlueprintAppToolkit;
import io.ast.jneurocarto.javafx.utils.Result;

@NullMarked
@SuppressWarnings("preview")
public class ScriptThread implements Runnable {
    private static final ScopedValue<ScriptThread> CURRENT = ScopedValue.newInstance();

    private final BlueprintAppToolkit<Object> toolkit;
    final BlueprintScriptCallable callable;
    final Object[] arguments;
    private final BiConsumer<ScriptThread, @Nullable Throwable> complete;

    private final Logger log = LoggerFactory.getLogger(ScriptThread.class);

    ScriptThread(BlueprintAppToolkit<Object> toolkit,
                 BlueprintScriptCallable callable,
                 Object[] arguments,
                 BiConsumer<ScriptThread, @Nullable Throwable> complete) {
        log.debug("script \"{}\" init", callable.name());
        this.toolkit = toolkit;
        this.callable = callable;
        this.arguments = arguments;
        this.complete = complete;
    }

    public String name() {
        return callable.name();
    }

    @Override
    public void run() {
        log.debug("script \"{}\" start", name());

        try {
            complete(ScopedValue.where(CURRENT, this).call(this::invokeCallable));
        } catch (Throwable e) {
            complete(e);
        }
    }

    private @Nullable Throwable invokeCallable() throws Throwable {
        callable.invoke(toolkit, arguments);
        return null;
    }

    private void complete(@Nullable Throwable error) {
        log.debug("script \"{}\" complete with error {}", name(), Objects.toString(error));

        Platform.runLater(() -> {
            try {
                complete.accept(this, error);
            } catch (Exception e) {
                log.warn("complete. but ignored.", e);
            }
        });
    }

    public static @Nullable ScriptThread current() {
        return CURRENT.orElse(null);
    }

    public BlueprintScriptCallable callable() {
        return callable;
    }

    public Object[] arguments() {
        return arguments;
    }

    @SuppressWarnings("LoggingSimilarMessage")
    public static void awaitFxApplicationThread(Runnable runnable) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException();
        }

        var current = current();
        if (current == null) throw new IllegalStateException("not in a script thread");


        if (Platform.isFxApplicationThread()) {
            current.log.trace("{} awaitFxApplicationThread (run)", current.name());
            runnable.run();
            return;
        }

        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            current.log.trace("{} awaitFxApplicationThread (run)", current.name());
            try {
                runnable.run();
            } finally {
                latch.countDown();
            }
        });

        current.log.trace("{} awaitFxApplicationThread (wait)", current.name());
        latch.await();

        current.log.trace("{} awaitFxApplicationThread (return)", current.name());
    }

    @SuppressWarnings("LoggingSimilarMessage")
    public static <V> Result<V, Throwable> awaitFxApplicationThread(Callable<V> runnable) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            Thread.currentThread().interrupt();
            throw new InterruptedException();
        }

        var current = current();
        if (current == null) throw new IllegalStateException("not in a script thread");

        if (Platform.isFxApplicationThread()) {
            current.log.trace("{} awaitFxApplicationThread (run)", current.name());
            return Result.invoke(runnable);
        }

        var ret = new AtomicReference<Result<V, Throwable>>();
        var latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            current.log.trace("{} awaitFxApplicationThread (run)", current.name());
            try {
                ret.set(Result.invoke(runnable));
            } finally {
                latch.countDown();
            }
        });

        current.log.trace("{} awaitFxApplicationThread (wait)", current.name());
        latch.await();

        current.log.trace("{} awaitFxApplicationThread (return)", current.name());
        return ret.get();
    }
}
