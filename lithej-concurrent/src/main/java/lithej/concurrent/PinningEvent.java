package lithej.concurrent;

import java.time.Duration;

/**
 * One occurrence of a virtual thread being <em>pinned</em> to its carrier platform
 * thread during a {@link TaskScope}'s execution, captured via the JDK Flight Recorder
 * {@code jdk.VirtualThreadPinned} event.
 *
 * <p>Pinning defeats much of the point of virtual threads: a pinned virtual thread
 * occupies a real OS carrier thread for the duration of the block, exactly like a
 * platform thread would. A small number of brief, rare pinning events is usually
 * harmless; frequent or long pinning under load hurts scalability.
 *
 * <p><b>On JDK 21-23</b>, blocking while holding a monitor (a {@code synchronized}
 * method or statement) is the most common cause of pinning; replacing that
 * {@code synchronized} block with a {@link java.util.concurrent.locks.ReentrantLock}
 * (which does not pin) is the standard fix. <b>On JDK 24 and later</b>,
 * <a href="https://openjdk.org/jeps/491">JEP 491</a> changed the JVM so that
 * {@code synchronized} generally no longer pins a blocked virtual thread at all — this
 * class and the JFR event it wraps are unaffected by that change (they still report
 * whatever pinning genuinely occurs, e.g. from a blocking native/JNI call), but on
 * JDK 24+ you should expect to see far fewer of these events for code that was written
 * assuming {@code synchronized} pins.
 *
 * @param threadName the name of the pinned virtual thread
 * @param duration how long the thread was pinned
 * @param stackTraceSummary the first few stack frames at the point of pinning, one
 *     per line, or an empty string if a stack trace was not available
 */
public record PinningEvent(String threadName, Duration duration, String stackTraceSummary) {
}
