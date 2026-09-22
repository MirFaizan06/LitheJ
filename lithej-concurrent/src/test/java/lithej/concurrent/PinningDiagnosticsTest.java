package lithej.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Verifies {@link TaskScope#pinningEvents()} against a real, deliberately-triggered
 * pinning event, not a mock or assumption about JFR behavior.
 *
 * <p>Every test here explicitly calls {@link TaskScope#close()} before inspecting
 * {@link TaskScope#pinningEvents()}, rather than relying on try-with-resources' close
 * at the end of the block: {@code close()} is what flushes JFR's buffered events (via
 * {@code RecordingStream.stop()}), so checking events before it has actually run is a
 * race, not a guarantee -- exactly as documented on {@link TaskScope#pinningEvents()}.
 *
 * <p><b>JDK 24 changes what "pinning" even means here.</b> <a
 * href="https://openjdk.org/jeps/491">JEP 491</a> (delivered in JDK 24) changed
 * {@code synchronized} so that it generally no longer pins a blocked virtual thread to
 * its carrier at all -- the exact scenario this test suite uses to trigger a pinning
 * event on purpose. Rather than let that make this suite flaky or silently
 * meaningless on newer JDKs, the two scenario-specific tests below are gated by JDK
 * version and there is a dedicated test that pins the JEP 491 behavior itself, so this
 * suite accurately documents and verifies the real behavior on both sides of that
 * change instead of assuming one era's behavior everywhere.
 */
class PinningDiagnosticsTest {

    private static final Object MONITOR = new Object();

    /** True on JDK 21-23, where {@code synchronized} still pins a blocked virtual thread. */
    static boolean synchronizedBlocksStillPin() {
        return Runtime.version().feature() < 24;
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @EnabledIf("synchronizedBlocksStillPin")
    void detectsARealPinningEventCausedBySynchronizedAroundABlockingCall() {
        TaskScope scope = Concurrency.scope();
        try {
            scope.fork(() -> {
                synchronized (MONITOR) {
                    // Holding a monitor while a virtual thread blocks is what pins it
                    // to its carrier thread on JDK 21-23 (see the class Javadoc for
                    // why this test is gated to those versions); Thread.sleep is a
                    // simple, reliable way to force a blocking operation here.
                    Thread.sleep(300);
                }
                return null;
            });
            scope.joinAll();
        } finally {
            scope.close();
        }

        List<PinningEvent> events = scope.pinningEvents();
        assertThat(events).as("expected the synchronized+sleep task to be reported as pinned").isNotEmpty();

        PinningEvent event = events.get(0);
        assertThat(event.threadName()).isNotBlank();
        assertThat(event.duration()).isNotNull();
        assertThat(event.duration().toMillis()).isGreaterThan(0);
        assertThat(event.stackTraceSummary()).isNotBlank();
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisabledIf("synchronizedBlocksStillPin")
    void synchronizedNoLongerPinsUnderJep491OnJdk24AndLater() {
        TaskScope scope = Concurrency.scope();
        try {
            scope.fork(() -> {
                synchronized (MONITOR) {
                    Thread.sleep(300);
                }
                return null;
            });
            scope.joinAll();
        } finally {
            scope.close();
        }

        // The mechanism (JFR jdk.VirtualThreadPinned listening) is unchanged; there is
        // simply nothing to detect here on JDK 24+, because the JVM itself no longer
        // pins in this scenario. This test exists so a JDK 24+ run of this suite still
        // exercises and documents the real, current behavior instead of silently
        // skipping the concept of "does synchronized still pin?" entirely.
        assertThat(scope.pinningEvents())
                .as("JDK 24+ (JEP 491): synchronized no longer pins a blocked virtual thread here")
                .isEmpty();
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void reportsNoPinningForATaskThatNeverHoldsAMonitorWhileBlocking() {
        TaskScope scope = Concurrency.scope();
        try {
            scope.fork(() -> {
                // No synchronized block: a virtual thread blocking here unmounts from
                // its carrier instead of pinning it, on every supported JDK version.
                Thread.sleep(300);
                return null;
            });
            scope.joinAll();
        } finally {
            scope.close();
        }

        assertThat(scope.pinningEvents()).isEmpty();
    }

    @Test
    void pinningEventsIsEmptyForAScopeThatNeverForkedAnything() {
        try (TaskScope scope = Concurrency.scope()) {
            scope.joinAll();
            assertThat(scope.pinningEvents()).isEmpty();
        }
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @EnabledIf("synchronizedBlocksStillPin")
    void pinningEventsFromOneScopeAreNotAttributedToAConcurrentlyRunningUnrelatedScope() {
        // Guards against the JFR RecordingStream being process-wide: a pinning event
        // from scope B's task must not show up in scope A's pinningEvents(). Gated to
        // JDK 21-23 for the same reason as detectsARealPinningEventCausedBy...: it
        // needs a scenario that actually produces at least one pinning event to prove
        // isolation against, and synchronized+sleep only reliably provides one there.
        TaskScope scopeA = Concurrency.scope();
        try {
            scopeA.fork(() -> {
                Thread.sleep(50); // deliberately not pinning
                return null;
            });

            TaskScope scopeB = Concurrency.scope();
            try {
                scopeB.fork(() -> {
                    synchronized (MONITOR) {
                        Thread.sleep(300);
                    }
                    return null;
                });
                scopeB.joinAll();
            } finally {
                scopeB.close();
            }
            assertThat(scopeB.pinningEvents()).isNotEmpty();

            scopeA.joinAll();
        } finally {
            scopeA.close();
        }
        assertThat(scopeA.pinningEvents()).isEmpty();
    }
}
