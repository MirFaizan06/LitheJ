# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial implementation of all eight modules: `lithej-core`, `lithej-collections`,
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
  artifacts, a GitHub Release with checksums, and (when credentials are configured)
  a Maven Central publish.

[Unreleased]: https://github.com/{{GITHUB_USERNAME}}/lithej/compare/v0.0.0...HEAD
