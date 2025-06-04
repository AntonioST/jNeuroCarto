package io.ast.jneurocarto.javafx.utils;

import java.util.concurrent.Callable;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public sealed interface Result<V, X extends Throwable> {
    record Success<V, X extends Throwable>(V value) implements Result<V, X> {
    }

    record Failure<V, X extends Throwable>(X error) implements Result<V, X> {
    }

    static <V, X extends Throwable> Success<V, X> success(V value) {
        return new Success<>(value);
    }

    static <V, X extends Throwable> Failure<V, X> fail(X error) {
        return new Failure<>(error);
    }

    static Result<@Nullable Void, Throwable> invoke(Runnable runnable) {
        try {
            runnable.run();
            return success(null);
        } catch (Throwable error) {
            return fail(error);
        }
    }

    static <V> Result<V, Throwable> invoke(Callable<V> callable) {
        try {
            return success(callable.call());
        } catch (Throwable error) {
            return fail(error);
        }
    }

    default boolean isSuccess() {
        return this instanceof Result.Success<V, X>;
    }

    default boolean isFailure() {
        return this instanceof Result.Failure<V, X>;
    }

    default <R> Result<R, Throwable> then(Function<V, R> mapper) {
        return switch (this) {
            case Success(var value) -> success(mapper.apply(value));
            case Failure(var error) -> fail(error);
        };
    }

    default Result<V, X> rescue(Function<X, V> mapper) {
        return switch (this) {
            case Success success -> success;
            case Failure(var error) -> success(mapper.apply(error));
        };
    }

    default V getOrThrow() throws X {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> throw error;
        };
    }

    default V getOrThrow(Function<Throwable, RuntimeException> exception) {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> throw exception.apply(error);
        };
    }

    default V getOrThrowRuntimeException() {
        return switch (this) {
            case Success(var value) -> value;
            case Failure(var error) -> throw new RuntimeException(error);
        };
    }
}
