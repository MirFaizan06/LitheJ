# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.0] - 2026-09-23

### Added

- **`lithej-concurrent`** (new module, Java 21+): leak-proof structured concurrency
  for virtual threads, built entirely on Java 21's stable APIs rather than the JDK's
  own preview `StructuredTaskScope`.
  - `Concurrency.scope()` / `Concurrency.scope(FailurePolicy)` create a `TaskScope`
    that forks work via `fork(Callable)` and guarantees, structurally, that no forked
    task can outlive the scope: `close()` always joins every forked task, whether or
    not `joinAll()` was ever called, and never silently discards a task failure.
  - `FailurePolicy.FAIL_FAST` (default): the first task failure cancels every other
    running task in the scope. `FailurePolicy.COLLECT_ALL`: every task runs to
    completion regardless of individual outcomes; every failure is reported together.
  - Zero-configuration virtual-thread pinning diagnostics: while a scope has forked
    tasks, it listens for `jdk.VirtualThreadPinned` JFR events and exposes them via
    `pinningEvents()` — no JVM flags, no separate profiling run required. Correctly
    accounts for [JEP 491](https://openjdk.org/jeps/491) (JDK 24+), which changed
    `synchronized` to generally no longer pin virtual threads.
  - This module is its own opt-in artifact and is deliberately **not** a dependency
    of the `lithej` aggregate, so depending on the aggregate never forces a Java 21
    requirement.

### Changed

- CI's JDK 25 matrix leg now skips SpotBugs and PMD specifically
  (`-Dspotbugs.skip=true -Dpmd.skip=true`): as of the pinned tool versions, neither
  can run *on* a JDK 25 JVM yet (both fail trying to resolve JDK 25's own runtime
  classes). This is a limitation of those tools, not of LitheJ's code — the full test
  suite, Checkstyle, JaCoCo, and Javadoc all continue to run and pass on JDK 25. See
  `CONTRIBUTING.md` for details.
- `central-publishing-maven-plugin` 0.7.0 → 0.11.0, and the release workflow's Maven
  Central credential wiring was corrected to use a generated `settings.xml`
  `<server>` block (via `actions/setup-java`'s `server-id`/`server-username`/
  `server-password` inputs), which `publishingServerId` actually requires. The
  release profile's `autoPublish` is now `true` (was `false` for the very first
  release only, to allow a manual sanity check before anything went live).

## [1.0.0] - 2026-09-22

### Added

- Initial implementation of eight modules: `lithej-core`, `lithej-collections`,
  `lithej-io`, `lithej-time`, `lithej-net`, `lithej-async`, `lithej-config`, and the
  `lithej` aggregate.
- `lithej-core`: `ObjectsX`, `Validate`, `Result<T, E>`, `Numbers`, `Text`,
  `Console`/`ConsoleIO`.
- `lithej-collections`: `Lists`, `Maps`, `Sets`, `Iterables`.
- `lithej-io`: `FileIO`, `Directories`, `Resources`, `lithej.process.ProcessX`.
- `lithej-time`: `Times`.
- `lithej-net`: `Http`, `HttpOptions`, `HttpResponse`.
- `lithej-async`: `Async`.
- `lithej-config`: `Env`, `PropertiesX`, `Config`.
- Quality gates: JUnit 5 test suites per module, Checkstyle, SpotBugs, PMD, JaCoCo
  coverage gate (85% line / 75% branch minimum), Javadoc generation, all wired into
  `./mvnw verify`.
- CI: GitHub Actions matrix across Java 17/21/25 on Ubuntu, Windows, and macOS.
- Release automation: tag-triggered GitHub Actions workflow producing signed
  artifacts, a GitHub Release with checksums, and a Maven Central publish.

[Unreleased]: https://github.com/MirFaizan06/lithej/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/MirFaizan06/lithej/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/MirFaizan06/lithej/releases/tag/v1.0.0
