# Security Policy

## Supported Versions

LitheJ follows semantic versioning. Until `1.0.0` is released, only the latest
`0.x` release receives security fixes. Once `1.0.0` ships, the latest minor
release of the current and previous major version will receive security
patches.

| Version | Supported          |
| ------- | ------------------- |
| latest  | :white_check_mark:  |
| older   | :x:                  |

## Reporting a Vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Instead, use GitHub's private vulnerability reporting: go to the
[Security tab](https://github.com/{{GITHUB_USERNAME}}/lithej/security) of this
repository and select "Report a vulnerability." If that is unavailable, email
{{DEVELOPER_EMAIL}} with a description of the issue, steps to reproduce, and
its potential impact.

We aim to acknowledge reports within 5 business days and to release a fix or
mitigation plan within 30 days for confirmed issues, depending on severity and
complexity.

## Scope and Known Risk Areas

LitheJ is a general-purpose convenience library, not a security product, but
several of its APIs touch operations that are security-sensitive by nature.
The following areas have been specifically reviewed during development; if you
find a gap, please report it:

- **Path traversal.** `FileIO`/`Directories`/`Resources` accept caller-supplied
  paths and resource names as-is; they do not sandbox or restrict them to a
  base directory. If your application accepts a file path or resource name
  from an untrusted user, validate/sanitize it (e.g. reject `..` segments or
  resolve against a known base directory and verify containment) before
  passing it to LitheJ — the same discipline you'd apply to `java.nio.file`
  or `ClassLoader` APIs directly, which these wrap.
- **Unsafe recursive deletion.** `Directories.deleteRecursive` and
  `Directories.empty` permanently delete file trees with no confirmation step
  and no recovery mechanism. Their names are deliberately explicit about this.
  Never call them with a caller-controlled path without validating it first.
- **Accidental overwrite.** `FileIO.copy`/`FileIO.move` refuse to overwrite an
  existing destination file unless you explicitly pass `overwrite = true`.
  `FileIO.write`/`writeLines`/`writeBytes`, like `Files.writeString`, do
  overwrite an existing file by design — this is documented on each method.
- **Command execution.** `lithej.process.ProcessX` passes each command
  argument to the OS as a separate, unshelled argument (no shell is invoked
  unless you explicitly build a command like `List.of("sh", "-c", ...)`).
  Passing untrusted input into a command line you control is still risky the
  same way it is with a bare `ProcessBuilder` — LitheJ does not add shell
  injection risk beyond what `ProcessBuilder` already has, but it does not
  remove it either.
- **HTTP redirect behavior.** `lithej.net.Http` uses `java.net.http.HttpClient`
  with `HttpClient.Redirect.NORMAL` by default (follows redirects, but not
  from HTTPS to HTTP). Supply your own pre-configured `HttpClient` via
  `HttpOptions.withClient(...)` if you need a stricter policy (e.g.
  `Redirect.NEVER`) for requests to untrusted URLs.
- **Resource leaks.** Methods that return a `Stream<Path>` or `InputStream`
  document that the caller must close them; eager, list-returning
  alternatives (`FileIO.walk`, `FileIO.find`, `Resources.read*`) are provided
  specifically to avoid leaks in the common case.
- **Unbounded input/output capture.** `ProcessX` and `Http` capture process
  output / response bodies entirely in memory; this is appropriate for
  typical CLI tools and API responses, but is not suitable for untrusted
  processes/endpoints that might return gigabytes of data. There is no
  built-in size cap — if you're consuming untrusted output, consider a
  timeout (both modules support one) as a partial mitigation, since neither
  module currently caps captured bytes.
- **Unbounded concurrency.** `lithej.async.Async` never creates its own
  unbounded thread pool; every method either takes an explicit `Executor` or
  clearly documents that it uses `ForkJoinPool.commonPool()` (the same
  default `CompletableFuture.supplyAsync` uses).
- **Secret leakage in logs.** `lithej.process.ProcessOptions`,
  `lithej.net.HttpOptions`, and `lithej.net.HttpResponse` redact values for
  header/environment-variable names that look like secrets (containing
  `authorization`, `cookie`, `token`, `secret`, `key`, or `password`,
  case-insensitively) in their `toString()` output. This is a best-effort
  heuristic, not a guarantee — it will not catch a secret stored under an
  unrecognized name. Do not rely on it as your only safeguard against
  logging secrets; avoid logging full options/response objects that may
  carry credentials under non-standard names.
- **Temporary file handling.** `FileIO.tempFile`/`tempDirectory` use
  `java.nio.file.Files.createTempFile`/`createTempDirectory`, which create
  files with permissions restricted to the current user on POSIX systems (per
  the JDK's own documented behavior) and derive names from a
  cryptographically-unpredictable counter, not a guessable sequence.

LitheJ deliberately does not implement any deserialization of untrusted binary
data (no custom `ObjectInputStream` usage, no XML/YAML parsing), so it is not
exposed to the classic unsafe-deserialization vulnerability class.
