# LitheJ

LitheJ is a small, dependency-free Java library that makes common, repetitive Java
tasks — console input, file I/O, collection transformations, text processing, HTTP
calls, config loading — shorter and safer to write, without hiding how Java works or
locking you into a proprietary framework.

```java
int age = Console.askInt("Enter age: ");

List<Integer> numbers = Lists.of(1, 2, 3, 4, 5);
List<Integer> even = Lists.filter(numbers, n -> n % 2 == 0);

List<String> lines = FileIO.readLines(Path.of("users.txt"));
```

## New in 1.1: leak-proof structured concurrency

Most of LitheJ is a thin, honest layer over JDK APIs that are already simple — on
purpose (see [Design philosophy](#design-philosophy)). `lithej-concurrent` is
different: it closes a real, current gap. The JDK's own structured-concurrency API
(`StructuredTaskScope`) has stayed in preview through every release since virtual
threads went stable in JDK 21, so there has been no *stable* way to get its actual
benefit — making a leaked background thread structurally impossible instead of a
code-review hope — without depending on an API that keeps changing shape. It also
ships zero-configuration virtual-thread pinning diagnostics, something no other
library packages this simply.

```java
try (TaskScope scope = Concurrency.scope()) {
    Subtask<User> user   = scope.fork(() -> fetchUser(id));
    Subtask<List<Order>> orders = scope.fork(() -> fetchOrders(id));

    scope.joinAll(); // waits for both; first failure cancels the other

    return new Profile(user.get(), orders.get());
} // guarantee: no forked task can outlive this block. Ever. Not "usually."
```

Requires Java 21+ and is its own opt-in artifact (`lithej-concurrent`) — it does not
raise the rest of the library's Java 17 baseline, and the `lithej` aggregate
deliberately does not depend on it. See [Concurrent examples](#concurrent-examples)
below.

## Why this exists

Ordinary Java tasks — reading a validated integer from stdin, reading a UTF-8 file,
filtering a list, joining strings — take more boilerplate than they should, and that
boilerplate is exactly where small bugs (wrong charset, forgotten resource close,
off-by-one loop) creep in. LitheJ wraps these tasks in a small, consistent API built
entirely on top of the JDK: every method accepts and returns standard types
(`List`, `Map`, `Path`, `Optional`, `Duration`, `CompletableFuture`, ...), so you can
mix LitheJ calls with plain Java freely, drop back to the JDK API at any point, and
never wonder what a LitheJ method is hiding from you.

LitheJ is **not** a competing collections framework, date/time library, HTTP client,
or async runtime — each module is a thin, honest layer over the matching JDK API,
and reaches for `java.util.stream`, `java.time`, `java.net.http`, or
`java.util.concurrent` directly wherever that's already simple enough.

## Requirements

- **Java 17 or newer** for every module except `lithej-concurrent`, which needs
  **Java 21+** (it uses virtual threads). LitheJ does not use any API newer than
  Java 17 in its Java 17-baseline modules, so those run unmodified on 17, 21, 25, and
  later.
- No required runtime dependencies beyond the JDK for any module.
  `lithej-net`'s tests (not its runtime) depend on OkHttp's MockWebServer; the
  shipped `lithej-net` jar itself has zero runtime dependencies.

## Installation

These coordinates will work once the corresponding version is actually published to
Maven Central or JitPack — see [CONTRIBUTING.md](CONTRIBUTING.md) for the publishing
setup.

### Maven

Depend on just what you need:

```xml
<dependency>
    <groupId>io.github.mirfaizan06</groupId>
    <artifactId>lithej-core</artifactId>
    <version>1.1.0</version>
</dependency>
```

...or pull in every Java 17-baseline module with the aggregate artifact:

```xml
<dependency>
    <groupId>io.github.mirfaizan06</groupId>
    <artifactId>lithej</artifactId>
    <version>1.1.0</version>
</dependency>
```

`lithej-concurrent` (Java 21+, see [above](#new-in-11-leak-proof-structured-concurrency))
is opt-in and not pulled in by the aggregate:

```xml
<dependency>
    <groupId>io.github.mirfaizan06</groupId>
    <artifactId>lithej-concurrent</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("io.github.mirfaizan06:lithej-core:1.1.0")
// or
implementation("io.github.mirfaizan06:lithej:1.1.0")
// and/or, separately (Java 21+):
implementation("io.github.mirfaizan06:lithej-concurrent:1.1.0")
```

### JitPack (fallback, no Maven Central account needed)

If you'd rather not wait on a Central publish, every tagged release is also
installable via [JitPack](https://jitpack.io):

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
<dependency>
    <groupId>com.github.MirFaizan06</groupId>
    <artifactId>lithej</artifactId>
    <version>v1.0.0</version>
</dependency>
```

```kotlin
repositories {
    maven("https://jitpack.io")
}
dependencies {
    implementation("com.github.MirFaizan06:lithej:v1.0.0")
}
```

No installer, no code generation, no annotation processor, no framework
bootstrapping — add the dependency and start calling static methods.

## Quick start

```java
import lithej.console.Console;
import lithej.collections.Lists;
import lithej.io.FileIO;
import java.nio.file.Path;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String name = Console.ask("What's your name? ");
        Console.println("Hello, " + name + "!");

        List<Integer> numbers = Lists.of(1, 2, 3, 4, 5, 6);
        List<Integer> even = Lists.filter(numbers, n -> n % 2 == 0);
        Console.println("Even numbers: " + even);

        FileIO.write(Path.of("greeting.txt"), "Hello, " + name + "!\n");
    }
}
```

## Console examples

```java
int age = Console.askInt("Enter age: ");                 // retries until a valid int
boolean sure = Console.confirm("Proceed?");               // "Proceed? (y/n): "
String color = Console.choose("Pick a color:",
        List.of("red", "green", "blue"));                 // numbered menu, validated
String password = Console.readPassword("Password: ");     // masked when a real console is attached
```

`Console` is a static facade over `System.in`/`System.out`, convenient for scripts.
For anything you want to unit test, construct a `ConsoleIO` over your own streams
instead — it has the exact same methods, but reads/writes the streams you give it:

```java
var in = new ByteArrayInputStream("42\n".getBytes(UTF_8));
var out = new ByteArrayOutputStream();
ConsoleIO console = new ConsoleIO(in, new PrintStream(out, true, UTF_8));

int age = console.askInt("Enter age: ");
assertEquals(42, age);
```

## Files examples

```java
String text = FileIO.read(path);                    // UTF-8, whole file
List<String> lines = FileIO.readLines(path);
FileIO.write(path, "content");                       // creates parent dirs, overwrites
FileIO.append(path, "more content\n");
FileIO.copy(source, target);                         // refuses to overwrite by default
FileIO.copy(source, target, true);                   // explicit opt-in to overwrite
FileIO.writeAtomic(path, "content");                 // write-to-temp-then-move

Directories.ensureExists(dir);
List<Path> files = FileIO.walk(dir);                 // recursive, regular files only
Directories.deleteRecursive(dir);                    // explicit, destructive, documented

String config = Resources.read("config/defaults.properties"); // classpath, works inside a JAR
```

## Collections examples

```java
List<Integer> numbers = Lists.of(1, 2, 3, 4, 5, 6, 7, 8);

List<Integer> even = Lists.filter(numbers, n -> n % 2 == 0);
List<List<Integer>> chunks = Lists.chunk(numbers, 3);            // [[1,2,3],[4,5,6],[7,8]]
Map<Boolean, List<Integer>> split = Lists.partition(numbers, n -> n > 4);
Map<String, List<Integer>> byParity = Lists.groupBy(numbers, n -> n % 2 == 0 ? "even" : "odd");
Optional<Integer> max = Lists.maxBy(numbers, Comparator.naturalOrder());

Map<String, Integer> scores = Map.of("alice", 90, "bob", 75);
Map<Integer, String> byScore = Maps.invert(scores);              // throws on duplicate values
Map<String, Integer> passing = Maps.filterValues(scores, v -> v >= 80);

Set<Integer> a = Sets.of(1, 2, 3);
Set<Integer> b = Sets.of(2, 3, 4);
Set<Integer> shared = Sets.intersection(a, b);                   // {2, 3}
```

## Text examples

```java
Text.isBlank("   ");                       // true
Text.words("  hello   world  ");           // ["hello", "world"]
Text.titleCase("hello world");             // "Hello World"
Text.truncate("hello world", 5);           // "hello"
Text.ellipsis("hello world", 8);           // "hello..."
Text.padLeft("7", 3, '0');                 // "007"
Text.between("<a>value</a>", "<a>", "</a>"); // Optional["value"]
```

## HTTP examples

```java
HttpResponse response = Http.get("https://api.example.com/users/1");
if (response.isSuccessful()) {
    String body = response.body();
}

HttpOptions options = HttpOptions.defaults()
        .withHeader("Authorization", "Bearer " + token)
        .withQueryParam("verbose", "true")
        .withTimeout(Duration.ofSeconds(10));

HttpResponse created = Http.post("https://api.example.com/users", "{\"name\":\"Ada\"}", options);
```

`Http` never invents retries, cookie jars, or auth flows — for anything beyond a
single request/response, build and pass in your own `java.net.http.HttpClient` via
`HttpOptions.withClient(...)`, and reach the raw JDK response via
`response.raw()` any time you need something LitheJ didn't wrap.

## Concurrent examples

Requires the separate `lithej-concurrent` artifact and Java 21+.

```java
try (TaskScope scope = Concurrency.scope()) {
    Subtask<String> a = scope.fork(() -> callServiceA());
    Subtask<String> b = scope.fork(() -> callServiceB());

    scope.joinAll(); // waits for both; a failure in either cancels the other

    combine(a.get(), b.get());
} // guarantee: neither forked task can still be running once this line executes
```

Don't want the first failure to cancel everything? Collect every failure instead:

```java
try (TaskScope scope = Concurrency.scope(FailurePolicy.COLLECT_ALL)) {
    List<Subtask<Report>> reports = ids.stream().map(id -> scope.fork(() -> generate(id))).toList();
    try {
        scope.joinAll();
    } catch (MultipleTaskFailuresException e) {
        e.failures().forEach(failure -> log.warn("report generation failed", failure));
    }
    List<Report> succeeded = reports.stream()
            .filter(r -> r.state() == Subtask.State.SUCCESS)
            .map(Subtask::get)
            .toList();
}
```

Zero-configuration pinning diagnostics — check `pinningEvents()` after `close()` has
returned, since that's what flushes JFR's buffered events:

```java
TaskScope scope = Concurrency.scope();
try {
    scope.fork(() -> riskyLegacyCodeThatMightStillUseSynchronized());
    scope.joinAll();
} finally {
    scope.close();
}
scope.pinningEvents().forEach(e ->
        log.warn("virtual thread pinned for {} at {}", e.duration(), e.stackTraceSummary()));
```

## Error handling

Two complementary tools, used for different kinds of failure:

**Exceptions** (mostly unchecked, always documented) for genuine failures — a missing
file, a malformed number, a network error. Every method's Javadoc states exactly what
it throws.

**`Result<T, E>`** for expected, recoverable outcomes you want to handle without
exceptions:

```java
OptionalInt parsedInt = Numbers.tryInt(input);
Result<Integer, String> parsed = parsedInt.isPresent()
        ? Result.success(parsedInt.getAsInt())
        : Result.failure("not a number: " + input);

int value = parsed.orElse(0);
parsed.ifFailure(error -> Console.println("Warning: " + error));

Result<String, Exception> attempt = Result.of(() -> Files.readString(path));
```

## API modules

| Module | Package(s) | Contents |
|---|---|---|
| `lithej-core` | `lithej.core`, `lithej.text`, `lithej.console` | `ObjectsX`, `Validate`, `Result`, `Numbers`, `Text`, `Console`/`ConsoleIO` |
| `lithej-collections` | `lithej.collections` | `Lists`, `Maps`, `Sets`, `Iterables` |
| `lithej-io` | `lithej.io`, `lithej.process` | `FileIO`, `Directories`, `Resources`, `ProcessX` |
| `lithej-time` | `lithej.time` | `Times` |
| `lithej-net` | `lithej.net` | `Http`, `HttpOptions`, `HttpResponse` |
| `lithej-async` | `lithej.async` | `Async` |
| `lithej-config` | `lithej.config` | `Env`, `PropertiesX`, `Config` |
| `lithej-concurrent` (Java 21+) | `lithej.concurrent` | `Concurrency`, `TaskScope`, `Subtask`, `FailurePolicy`, `PinningEvent` |
| `lithej` | — | Aggregate: depends on every Java 17-baseline module above (not `lithej-concurrent` — opt in separately) |

Each module has one job and (beyond `lithej-core`, which most others depend on) can
be used independently. See the [documentation site](https://MirFaizan06.github.io/lithej/)
for the full guide and generated API reference for every class.

## Design philosophy

**Simple by default, powerful when needed.** A helper is added only when it makes
code meaningfully more readable, safer, or less error-prone than the equivalent
plain-Java code — not merely shorter. Where the JDK's own API is already simple, we
don't wrap it.

- **Predictable.** A method's name should tell you what it does; nothing here does
  something surprising you'd need to read the source to discover.
- **Minimal dependencies.** Every core module depends on nothing but the JDK.
- **Standard types in, standard types out.** You are never handed a LitheJ-specific
  collection, date/time, or HTTP type you can't pass straight into other Java code.
- **Explicit failure semantics.** Every method's Javadoc states its null behavior and
  what it throws. Destructive operations (`Directories.deleteRecursive`,
  `Directories.empty`) have names that say so.
- **No unbounded resource use.** Nothing here silently spawns unlimited threads,
  retries forever, or reads unbounded input by default.

## Thread-safety notes

Every public class's Javadoc states its thread-safety explicitly. As a summary:

- Stateless utility classes (`Text`, `Numbers`, `Validate`, `ObjectsX`, `Lists`,
  `Maps`, `Sets`, `Iterables`, `FileIO`, `Directories`, `Resources`, `Times`, `Http`,
  `Async`, `Env`, `PropertiesX`) are thread-safe — they hold no mutable state.
- Immutable value types (`Result`, `HttpOptions`, `ProcessOptions`, `HttpResponse`,
  `Config`) are thread-safe to share and reuse across threads.
- `ConsoleIO` (and, transitively, `Console`, which wraps one shared instance bound to
  `System.in`/`System.out`) is **not** thread-safe for concurrent reads — a console
  session is inherently single-reader.
- `TaskScope` allows `fork()` from multiple threads, but `joinAll()`/`close()` are
  meant to be called once each, from the thread that created the scope, after forking
  is done — see its Javadoc for the exact contract. `Subtask` is safe to read from any
  thread once the owning scope has joined.

## Version compatibility

LitheJ follows [Semantic Versioning](https://semver.org/). Before `1.0.0`, minor
versions may include breaking changes. From `1.0.0` onward, only major version bumps
break public API compatibility; this is checked automatically in CI via
[Revapi](https://revapi.org/) once a `1.0.0` baseline exists. See
[CHANGELOG.md](CHANGELOG.md) for release history.

## Contributing

Contributions are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md) for build
instructions, coding conventions, and the release process.

## License

LitheJ is licensed under the [Apache License, Version 2.0](LICENSE).
