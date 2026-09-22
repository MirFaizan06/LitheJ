/**
 * An ergonomic wrapper around {@link java.net.http.HttpClient} for common GET, POST,
 * PUT, and DELETE calls.
 *
 * <p>{@link lithej.net.Http} is a static facade that performs the request and returns
 * an {@link lithej.net.HttpResponse}. {@link lithej.net.HttpOptions} configures
 * per-request headers, query parameters, timeout, and (optionally) a caller-supplied
 * {@link java.net.http.HttpClient}. This package is deliberately not a competing HTTP
 * framework: it does not retry requests, manage cookies, or handle authentication for
 * you, and it always keeps the underlying JDK types (a raw
 * {@link java.net.http.HttpResponse}, a caller-built {@link java.net.http.HttpClient})
 * reachable for anything it doesn't wrap.
 */
package lithej.net;
