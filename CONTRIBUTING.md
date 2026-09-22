# Contributing to LitheJ

Thanks for considering a contribution. This document covers how to build and test the
project locally, the project's coding conventions, and (for maintainers) the exact
steps needed to configure release publishing.

## Building locally

Requirements: JDK 17 or newer (JDK 21 recommended for local development; CI verifies
17, 21, and 25).

```bash
git clone https://github.com/MirFaizan06/lithej.git
cd lithej
./mvnw clean verify
```

On Windows, use `.\mvnw.cmd clean verify` in PowerShell or `mvnw.cmd clean verify` in
cmd.exe.

`./mvnw clean verify` runs, for every module: compilation, unit tests, Checkstyle,
SpotBugs, PMD, the JaCoCo coverage gate, and Javadoc generation. All of these must
pass before a pull request can be merged.

To iterate faster while developing (skipping the slower quality gates), you can run:

```bash
./mvnw -pl lithej-core -am test -Dcheckstyle.skip=true -Dspotbugs.skip=true -Dpmd.skip=true
```

but always run a full `./mvnw clean verify` before opening a pull request.

## Project structure

LitheJ is a Maven multi-module project. See the root `pom.xml` for the module list and
[README.md](README.md#api-modules) for what each module contains. A short rule of
thumb: a new utility belongs in the module matching its domain (string helpers in
`lithej-core`'s `lithej.text` package, collection helpers in `lithej-collections`,
etc.); it does not belong in a new catch-all `Utils` class.

## Coding conventions

These aren't fully automated (Checkstyle catches some of it), so please follow them by
hand too:

- **Java 17 baseline.** Don't use APIs or language features newer than Java 17 in any
  module (the whole project targets 17; there is no per-module version split).
- **UTF-8 everywhere**, unless a method explicitly takes a `Charset` parameter.
- **Every public class, interface, record, constructor, and method needs Javadoc**:
  what it does, `@param`, `@return`, `@throws` (including null/empty/invalid-input
  behavior), and a thread-safety note on the class. Add a short `<pre>{@code ...}</pre>`
  example for anything non-trivial.
- **Null policy**: prefer throwing `NullPointerException` (via `lithej.core.Validate.notNull`)
  for missing required arguments, and `IllegalArgumentException` (via other `Validate`
  methods) for arguments that are present but invalid. Don't silently treat `null` as
  a default value unless a method's Javadoc says exactly that.
- **Don't reinvent the JDK.** A new helper needs to justify itself: does it make code
  meaningfully shorter, safer, or harder to misuse than the three-line JDK equivalent?
  If not, don't add it.
- **Destructive operations get unambiguous names** (`deleteRecursive`, `empty`, not
  `clean` or `reset`) and explicit Javadoc about what they destroy.
- **Never silently create unbounded threads, unbounded retries, or unbounded
  concurrency.** Anything that spawns work must take an `Executor`/timeout or have a
  narrowly-scoped, clearly documented default.

## Tests

- Use JUnit 5 and AssertJ (see any existing `*Test.java` for the house style of static
  imports).
- Cover the edges: null arguments, empty/blank input, Unicode, negative numbers and
  overflow boundaries, missing files, existing-destination-file conflicts, timeouts,
  and (where relevant) interrupted operations. Don't write a test that just calls a
  method without asserting anything meaningful.
- Filesystem tests must use JUnit 5's `@TempDir`, never a hardcoded path.
- Network tests (in `lithej-net`) must run against `MockWebServer`, never real hosts.
- The JaCoCo gate requires 85% line / 75% branch coverage per module (see
  `jacoco.line.minimum` / `jacoco.branch.minimum` in the root `pom.xml`); most modules
  currently exceed that comfortably. Don't write meaningless tests just to hit the
  number — if you genuinely can't cover a branch meaningfully, say so in the PR
  description rather than padding coverage.

## Known tooling limitation: SpotBugs/PMD on JDK 25

LitheJ's own source and tests are fully JDK 25-compatible — this was verified by
hand (compiling and running the complete test suite under a real Temurin 25 JVM, not
just assumed from the `maven.compiler.release=17` setting). However, two of the
static-analysis tools this project wires into `./mvnw verify` are not yet able to
*run on* a JDK 25 JVM as of the versions pinned in the root `pom.xml`
(`spotbugs-maven-plugin` 4.9.3.0, `pmd-plugin` 3.26.0 / PMD 7.7.0): both fail with
`Unsupported class file major version 69` / a parser error while trying to resolve
JDK 25's own runtime classes. This is a limitation of those tools, not of LitheJ's
code — Checkstyle, JaCoCo, and Javadoc generation all work correctly on JDK 25.

Practical effect: `./mvnw clean verify` run under a JDK 25 JVM will fail at the
SpotBugs or PMD step. CI works around this (see `.github/workflows/ci.yml`) by
skipping SpotBugs/PMD specifically when the matrix JDK is 25
(`-Dspotbugs.skip=true -Dpmd.skip=true`), while still running the full test suite,
Checkstyle, JaCoCo, and Javadoc on 25. If you're developing locally on JDK 25, use
the same flags, or develop against JDK 17/21 (both fully supported by every tool)
and only use JDK 25 to spot-check runtime behavior.

Re-check this whenever bumping `spotbugs-maven-plugin.version` or
`pmd-plugin.version` — remove the workaround once both tools officially support
running on JDK 25.

## Static analysis configuration

Checkstyle (`config/checkstyle/checkstyle.xml`), SpotBugs
(`config/spotbugs/spotbugs-exclude.xml`), and PMD (`config/pmd/pmd-ruleset.xml`) are
deliberately scoped to catch real bugs and genuine readability problems, not to
enforce a full stylistic rulebook. If a rule produces a false positive for a
legitimate pattern, fix the pattern if reasonable; if the rule itself doesn't fit this
codebase (as happened with PMD's `DoNotUseThreads` for `lithej.process.ProcessX`,
which legitimately manages its own I/O-draining threads), add a narrowly-scoped,
justified exclusion rather than disabling the whole category.

## Public API compatibility

Once version `1.0.0` ships, this project follows semantic versioning and treats
accidental breaking changes as bugs. A Revapi check (`org.revapi:revapi-maven-plugin`,
configured in the root `pom.xml`) compares each build against the previous released
version once a baseline exists; it is disabled by default (`revapi.skip=true`) until
there is a `1.0.0` to compare against. Maintainers should re-enable it
(`-Drevapi.skip=false`, or flip the default once released) after the first release and
review any reported breaking change before merging.

## Releasing (maintainers)

A release is triggered by pushing a tag like `v1.0.0`. `.github/workflows/release.yml`
then runs the full test/quality suite, packages the jar/sources/javadoc artifacts,
publishes a GitHub Release with checksums attached, and — only if the secrets below
are configured — signs and publishes to Maven Central.

### One-time setup: Maven Central (Central Publishing Portal)

Sonatype's publishing process and tooling have changed over the years (OSSRH →
Central Publishing Portal); the steps below were re-verified against
<https://central.sonatype.org/register/namespace/> and
<https://central.sonatype.org/publish/publish-portal-maven/> on 2026-09-22. **Re-verify
before your next release if it's been a while**, since this can change.

**Status for this project:** namespace/token — not yet done (see below). GPG
signing — done; see the next section.

1. Sign in at <https://central.sonatype.com/> with the GitHub account that owns this
   repository. **If you signed up via GitHub, the `io.github.MirFaizan06` namespace
   is very likely already auto-verified** — Sonatype grants `io.github.<your GitHub
   username>` automatically for GitHub-based signups. Check first: account menu →
   **View Namespaces**. If it's already listed as verified, skip to step 2.
   Otherwise, click "Add Namespace", enter `io.github.MirFaizan06`, and follow the
   in-portal instructions to create a short-lived verification repository (it can be
   deleted once Sonatype confirms it). If neither works, email
   central-support@sonatype.com.
2. Generate a **user token** (account menu → **Generate User Token**) — this gives
   you a username/password pair used by the publishing plugin, distinct from your
   login password. This step requires being logged into the portal, so only you can
   do it.
3. Add these repository secrets in **GitHub → Settings → Secrets and variables →
   Actions → New repository secret** (or ask an assistant with a GitHub token that
   has secrets-write access on this repo to add them, the same way the GPG secrets
   below were added — without ever putting the raw token in chat/commit history):
   - `CENTRAL_TOKEN_USERNAME` — the generated token's username
   - `CENTRAL_TOKEN_PASSWORD` — the generated token's password
4. `release.yml` wires these into a generated `settings.xml` `<server>` block (via
   `actions/setup-java`'s `server-id`/`server-username`/`server-password` inputs) that
   `central-publishing-maven-plugin`'s `publishingServerId` requires — plain env vars
   alone are not sufficient for that plugin, despite some older blog posts suggesting
   otherwise. See the comment in `release.yml` next to the `setup-java` step.
5. The release profile uses `autoPublish=false` deliberately: a release upload lands
   in the Central Portal as a pending deployment that a maintainer reviews and
   manually clicks "Publish" on, rather than going live automatically. Flip it to
   `true` in the root `pom.xml`'s `release` profile once you're confident in the
   pipeline and want fully unattended releases.

### One-time setup: artifact signing (GPG)

Maven Central requires every published artifact to be signed. **Done for this
project** — `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE` are already set as repository
secrets, generated and uploaded programmatically (private key material was written
only to a local temp file, uploaded via GitHub's encrypted-secret API, then deleted —
never printed in a terminal transcript or committed).

- **Key ID / fingerprint:** `43DD43645AD5536DAA6030567BAC9158BFF21A44`
  (short ID `7BAC9158BFF21A44`)
- **Identity:** `MirFaizan06 (LitheJ release signing) <mirfaizan8803@gmail.com>`
- **Type:** RSA 4096, signing-only
- **Expires:** 2028-09-21 — rotate before then (repeat the steps below, then update
  the `GPG_PRIVATE_KEY`/`GPG_PASSPHRASE` secrets and this section)
- **Published to:** keyserver.ubuntu.com and keys.openpgp.org, so Central can verify
  signatures against it

To rotate or replace this key:

1. Generate a new key pair:
   ```bash
   gpg --full-generate-key
   ```
   Choose RSA, 4096 bits, and a reasonable expiry (e.g. 2 years).
2. Publish the public key to a keyserver:
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys <YOUR_KEY_ID>
   ```
3. Export the private key:
   ```bash
   gpg --armor --export-secret-keys <YOUR_KEY_ID> > private-key.asc
   ```
4. Update the `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE` repository secrets with the new
   values.
5. **Delete `private-key.asc` from local disk** once it's stored as a secret; never
   commit it.
6. Update the key ID/fingerprint/expiry noted above.

### Free-tier eligibility

Maven Central's Central Publishing Portal is free for open-source projects; there is
no payment involved in the steps above. If a future policy change made this
project/account ineligible for free publishing, do not pay for it — publish only
through the free channels below and update this document to say so plainly, rather
than silently going without a working publish path.

### Completely free distribution paths (no account required beyond a free GitHub account)

Even without any of the Maven Central setup above, LitheJ remains fully installable:

- **GitHub Releases** — every tagged release attaches the jar, sources jar, javadoc
  jar, and a `SHA256SUMS.txt` checksum file automatically.
- **JitPack** ([jitpack.io](https://jitpack.io)) — builds directly from a GitHub tag
  with zero configuration beyond the `jitpack.yml` already in this repository. See the
  JitPack installation instructions in [README.md](README.md#jitpack-fallback). Verify
  a build actually succeeds at `https://jitpack.io/#MirFaizan06/lithej` after
  each tag — a successful `git push --tags` does not by itself prove JitPack built it.
- **GitHub Pages** — documentation is published automatically from `docs/` on every
  push to `main`.

## Reporting issues / requesting features

Use [GitHub Issues](https://github.com/MirFaizan06/lithej/issues). Please
include a minimal reproduction for bugs.

## Code of Conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you
agree to abide by it.
