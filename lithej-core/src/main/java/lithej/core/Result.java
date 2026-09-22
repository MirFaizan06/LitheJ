package lithej.core;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A value that is either a {@link Success} holding a result, or a {@link Failure}
 * holding an error, without using an exception for the failure path.
 *
 * <p>{@code Result} is useful for operations whose failure is an expected, recoverable
 * outcome (e.g. "the input didn't parse", "the remote call returned 404") rather than a
 * programming error. It is deliberately small: transformation via {@link #map} and
 * {@link #flatMap}, and extraction via {@link #get()}, {@link #orElse}, and
 * {@link #toOptional()} cover the overwhelming majority of use cases. For unrecoverable
 * or unexpected failures, prefer throwing an exception instead of wrapping it in a
 * {@code Result}.
 *
 * <pre>{@code
 * Result<Integer, String> parsed = Numbers.tryInt(input)
 *         .map(Result::<Integer, String>success)
 *         .orElseGet(() -> Result.failure("not a number: " + input));
 *
 * int value = parsed.orElse(0);
 * }</pre>
 *
 * <p>This is a sealed type with exactly two implementations, {@link Success} and
 * {@link Failure}, both of which are immutable records. Instances of {@code Result}
 * are therefore immutable and thread-safe whenever {@code T} and {@code E} are.
 *
 * @param <T> the type of a successful value
 * @param <E> the type of an error value
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    /**
     * Creates a successful result holding {@code value}.
     *
     * @param value the success value; may be {@code null}
     * @param <T> the success type
     * @param <E> the error type
     * @return a new {@link Success}
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed result holding {@code error}.
     *
     * @param error the error value; must not be {@code null}
     * @param <T> the success type
     * @param <E> the error type
     * @return a new {@link Failure}
     * @throws NullPointerException if {@code error} is {@code null}
     */
    static <T, E> Result<T, E> failure(E error) {
        Objects.requireNonNull(error, "error must not be null");
        return new Failure<>(error);
    }

    /**
     * Runs {@code supplier} and wraps the outcome: a returned value becomes a
     * {@link Success}, and a thrown exception becomes a {@link Failure} holding that
     * exception.
     *
     * @param supplier the operation to run
     * @param <T> the success type
     * @return a {@link Success} wrapping the returned value, or a {@link Failure}
     *     wrapping the thrown exception
     */
    static <T> Result<T, Exception> of(ThrowingSupplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    /**
     * Returns {@code true} if this is a {@link Success}.
     *
     * @return {@code true} if this result succeeded
     */
    boolean isSuccess();

    /**
     * Returns {@code true} if this is a {@link Failure}.
     *
     * @return {@code true} if this result failed
     */
    boolean isFailure();

    /**
     * Returns the success value.
     *
     * @return the success value
     * @throws NoSuchElementException if this is a {@link Failure}
     */
    T get();

    /**
     * Returns the error value.
     *
     * @return the error value
     * @throws NoSuchElementException if this is a {@link Success}
     */
    E getError();

    /**
     * Returns the success value, or {@code fallback} if this is a {@link Failure}.
     *
     * @param fallback the value to return on failure; may be {@code null}
     * @return the success value, or {@code fallback}
     */
    T orElse(T fallback);

    /**
     * Returns the success value, or the result of invoking {@code fallback} if this is a
     * {@link Failure}.
     *
     * @param fallback supplier invoked with the error when this is a {@link Failure}
     * @return the success value, or {@code fallback.apply(getError())}
     */
    T orElseGet(Function<? super E, ? extends T> fallback);

    /**
     * Returns the success value, or throws an exception produced from the error.
     *
     * @param exceptionMapper builds the exception to throw from the error value
     * @param <X> the exception type
     * @return the success value
     * @throws X if this is a {@link Failure}
     */
    <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> exceptionMapper) throws X;

    /**
     * Transforms the success value, leaving a {@link Failure} untouched.
     *
     * @param mapper transformation applied to the success value
     * @param <R> the new success type
     * @return a {@link Result} holding the transformed value, or the original error
     */
    <R> Result<R, E> map(Function<? super T, ? extends R> mapper);

    /**
     * Transforms the error value, leaving a {@link Success} untouched.
     *
     * @param mapper transformation applied to the error value
     * @param <F> the new error type
     * @return a {@link Result} holding the transformed error, or the original value
     */
    <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper);

    /**
     * Chains a follow-up operation that itself returns a {@link Result}, leaving a
     * {@link Failure} untouched.
     *
     * @param mapper transformation applied to the success value
     * @param <R> the new success type
     * @return the {@link Result} returned by {@code mapper}, or the original error
     */
    <R> Result<R, E> flatMap(Function<? super T, ? extends Result<R, E>> mapper);

    /**
     * Invokes {@code action} with the success value, if this is a {@link Success}.
     *
     * @param action the action to run on success
     */
    void ifSuccess(Consumer<? super T> action);

    /**
     * Invokes {@code action} with the error value, if this is a {@link Failure}.
     *
     * @param action the action to run on failure
     */
    void ifFailure(Consumer<? super E> action);

    /**
     * Converts this result to an {@link Optional}, discarding the error on failure.
     *
     * @return an {@link Optional} holding the success value, or empty on failure
     */
    Optional<T> toOptional();

    /**
     * A supplier whose {@link #get()} may throw a checked exception.
     *
     * @param <T> the supplied type
     */
    @FunctionalInterface
    interface ThrowingSupplier<T> {
        /**
         * Computes a value.
         *
         * @return the computed value
         * @throws Exception if the computation fails
         */
        T get() throws Exception;
    }

    /**
     * A successful {@link Result} holding {@code value}.
     *
     * @param value the success value; may be {@code null}
     * @param <T> the success type
     * @param <E> the error type
     */
    record Success<T, E>(T value) implements Result<T, E> {

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public E getError() {
            throw new NoSuchElementException("Result is a success, not a failure");
        }

        @Override
        public T orElse(T fallback) {
            return value;
        }

        @Override
        public T orElseGet(Function<? super E, ? extends T> fallback) {
            return value;
        }

        @Override
        public <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> exceptionMapper) {
            return value;
        }

        @Override
        public <R> Result<R, E> map(Function<? super T, ? extends R> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper) {
            return new Success<>(value);
        }

        @Override
        public <R> Result<R, E> flatMap(Function<? super T, ? extends Result<R, E>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public void ifSuccess(Consumer<? super T> action) {
            action.accept(value);
        }

        @Override
        public void ifFailure(Consumer<? super E> action) {
            // no-op: this is a success
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.ofNullable(value);
        }
    }

    /**
     * A failed {@link Result} holding {@code error}.
     *
     * @param error the error value; never {@code null}
     * @param <T> the success type
     * @param <E> the error type
     */
    record Failure<T, E>(E error) implements Result<T, E> {

        /**
         * Creates a failure.
         *
         * @param error the error value
         * @throws NullPointerException if {@code error} is {@code null}
         */
        public Failure {
            Objects.requireNonNull(error, "error must not be null");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public T get() {
            throw new NoSuchElementException("Result is a failure: " + error);
        }

        @Override
        public E getError() {
            return error;
        }

        @Override
        public T orElse(T fallback) {
            return fallback;
        }

        @Override
        public T orElseGet(Function<? super E, ? extends T> fallback) {
            return fallback.apply(error);
        }

        @Override
        public <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> exceptionMapper) throws X {
            throw exceptionMapper.apply(error);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R, E> map(Function<? super T, ? extends R> mapper) {
            return (Result<R, E>) this;
        }

        @Override
        public <F> Result<T, F> mapError(Function<? super E, ? extends F> mapper) {
            return new Failure<>(mapper.apply(error));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R, E> flatMap(Function<? super T, ? extends Result<R, E>> mapper) {
            return (Result<R, E>) this;
        }

        @Override
        public void ifSuccess(Consumer<? super T> action) {
            // no-op: this is a failure
        }

        @Override
        public void ifFailure(Consumer<? super E> action) {
            action.accept(error);
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }
    }
}
