package lithej.net;

import java.util.Locale;
import java.util.Set;

/**
 * Recognizes header names that commonly carry secrets, so that {@link HttpOptions}
 * and {@link HttpResponse} can avoid printing their values in {@code toString()}.
 *
 * <p>Matching is a case-insensitive substring test against a fixed list of fragments
 * ({@code authorization}, {@code cookie}, {@code token}, {@code secret}, {@code key},
 * {@code password}), so it also catches header names that are not exact matches for a
 * standard header, such as {@code X-Api-Key} or {@code Proxy-Authorization}.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
final class SensitiveHeaders {

    /** The placeholder printed in place of a redacted header value. */
    static final String REDACTED = "***";

    private static final Set<String> SENSITIVE_FRAGMENTS =
            Set.of("authorization", "cookie", "token", "secret", "key", "password");

    private SensitiveHeaders() {
    }

    /**
     * Returns {@code true} if {@code headerName} looks like it carries a secret.
     *
     * @param headerName the header name to check
     * @return {@code true} if {@code headerName} contains one of the sensitive
     *     fragments, ignoring case
     */
    static boolean isSensitive(String headerName) {
        String lower = headerName.toLowerCase(Locale.ROOT);
        for (String fragment : SENSITIVE_FRAGMENTS) {
            if (lower.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
