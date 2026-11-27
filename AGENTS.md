# AGENTS.md

Binding rules for every agent and every person working in this repository. kheck is the logging library of Davils:
typed records, routing, masking, sampling, plain and JSON layouts, sinks with bounded queues, statistics, health and
events, and bridges for SLF4J 2 and `java.util.logging`, for Kotlin on the JVM and on WebAssembly (`wasmJs`), built on
[Kore](https://github.com/davils-com/kore). It is published under the Apache License 2.0 and is built to the standard
it is used for, with security first.

## Repository

| Module | Artifact | Purpose |
|---|---|---|
| `kheck-api` | `com.davils:kheck-api` | The logger, its levels and names, and the typed events, fields, markers and values a library logs |
| `kheck-core` | `com.davils:kheck-core` | The engine: configuration, routing, filters, enrichment, masking, sampling, layouts, console and custom sinks, statistics, health, events |
| `kheck-slf4j` | `com.davils:kheck-slf4j` | The SLF4J 2 provider; JVM only |
| `kheck-jul` | `com.davils:kheck-jul` | The bridge of `java.util.logging`; JVM only |
| `kheck-knot` | `com.davils:kheck-knot` | The logging closed as a task of a knot shutdown and the failures of knot written as records |
| `kheck-testing` | `com.davils:kheck-testing` | Test support; never on a production class path |
| `kheck-bom` | `com.davils:kheck-bom` | The bill of materials |
| `kheck-benchmark` | - | JMH benchmarks of the hot paths; never published |
| `build-logic` | - | One convention plugin per Kreate concern; they hold nothing but `plugins { }` and `kreate { }` |

The root package of a module is `com.davils.kheck.<module without kheck- and without hyphens>`, such as
`com.davils.kheck.core`, and its Java module, named as `Automatic-Module-Name`, has the same name. The code is ported
from the logging of an internal Davils project, without its configuration binding and its file sink: kheck never
touches the file system, and a sink of the application decides where records go. It builds on the Davils libraries
that replace the common modules of that project: kontinuity for the resilience of sinks, krypt for keys and MACs,
knot for the environment, the platform and the shutdown, konfetti for events and kollect for meters and health.

## Targets

`kheck-api`, `kheck-core`, `kheck-knot` and `kheck-testing` are Kotlin Multiplatform libraries for
`jvm` and `wasmJs`; the Wasm tests run under Node.js. `kheck-slf4j` and `kheck-jul` have the `jvm` target alone.

- Code lives in `src/commonMain/kotlin`. What only the JVM offers, such as thread-locals, `StackWalker`, `System.out`
  and blocking, hides behind an `internal expect fun` in `com.davils.kheck.core.internal.platform`, in `<Role>.kt` with
  its `actual` in `<Role>.jvm.kt` and `<Role>.wasmJs.kt`; `kheck-api` has the same seam in
  `com.davils.kheck.api.internal.platform`. kheck declares no `expect` or `actual` class. What knot already detects,
  such as the host system, is read from knot and not detected again.
- JavaScript interop is written with `@JsFun` externals in `src/wasmJsMain/kotlin` only.
- The README lists what differs between the targets; a new difference is added there.

## Verify

```bash
./gradlew check
```

Nothing is done until it is green: compilation with warnings as errors, Detekt with the Kreate rule set and
`allRules` over main and test sources, the tests on the JVM and on Wasm, Kover at 97 % of lines, branches and
instructions, the API and semver check, the license header and changelog checks, every Trivy scan, the standards
audit and the assurance gate. `check` needs the network. The build runs on JDK 25.

## Code rules

The rules of Kore apply unchanged; the most important ones:

- **License header first**, byte for byte as in the existing files.
- **No comments.** Names, types and tests carry the intent.
- **KDoc on public API only**, with `@param`, `@return`, `@throws` and `@since`, stating behaviour on invalid input,
  limits, thread safety and security considerations.
- **No `else`**, at most two `return` statements per function, **at most two chained calls**, named arguments for
  calls with more than three arguments or several arguments of one type.
- **One top-level type per file; no package for a single file** except the root package and `internal` packages.
- **No `!!`, no `lateinit`, explicit API, no library prefix** in names, no public `data class`: a public value writes
  `equals`, `hashCode` and `toString` by hand and is listed in the `UseDataClass` excludes of `detekt.yaml`.
- **Ordered, read-only collections.** Public maps and sets of kheck keep the order of insertion through
  `orderedCopy()` of `kheck-api`; lists use `immutableList()` of Kore. `immutableMap()` and `immutableSet()` of Kore
  lose the order and are not used for fields, markers or declarations.
- **Atomics** from `kotlin.concurrent.atomics`; **randomness** is not used at all — sampling is deterministic, and the
  backoffs kheck builds with kontinuity are never jittered.
- **Nothing global.** No mutable singletons and no threads or timers of the library's own; the application hands the
  scope and the dispatcher to `logging`. The only process-wide state is `Slf4jBridge` and the thread-locals of the
  log context on the JVM, because SLF4J and thread-bound context are process-wide by nature. kheck never registers a
  shutdown hook; the application closes its logging, directly or through `kheck-knot`. Resilience instances, buses,
  registries and cryptography come from the application, built on its own scope, time source and policy.

## Logging rules

- **A call of a logger never fails and never blocks for long.** What exceeds the limits of `com.davils.kheck.api.Limits`
  is left out and counted in `omitted`, which every layout writes. Only `LogOverflow.Block` on the intake may wait,
  bounded by its `maxWait`.
- **Foreign code is contained.** Filters, validators of masking rules, listeners, health rules, formats and sinks run
  inside `contained` or `attempted` of Kore. A failing filter accepts, a failing masking drops the record, a failing
  sink is retried as configured and then handed to its fallback; every failure is counted and reported to the
  diagnostics. Cancellations and failures of the platform pass through.
- **Every loss is counted.** Dropped records of the intake and of every sink, suppressed records per reason, records
  after close, reentrant records and failed listeners appear in `LoggingStatistics`.

## Security rules

- **Escape untrusted text exactly once, at the sink.** The plain layout writes every message, field, name, thread and
  stack trace through the sanitizer of `format/text`; the JSON layouts escape through `JsonWriter`. Both use
  `SafeText.isSafe` of Kore for the classification of code points and shorten text above `Limits.MAX_TEXT_LENGTH`.
  Every `toString` that shows foreign text goes through `SafeText`.
- **Secrets never leak.** `LogValue.Secret` holds no value. Keys are `SecretKey`s of krypt, never shown, wiped on
  `destroy()`. No hand-written cryptography: MACs and digests come from krypt under the policy of the logging.
  A `toString` never shows a field value, the message of a cause or a key.
- **Masking fails closed.** A validator that throws drops the record.
- **Every sink has an injection test** with the hostile fragments of `kore-testing`.

## Tests

- Kotest `FunSpec` only, Kotest assertions only, no mocking library; fakes come from `kheck-testing` or a `fixture`.
- Tests live in `src/commonUnitTest/kotlin` in the package of the code they test; tests that need the JVM, such as
  crashes with a `VirtualMachineError`, threads, `StackWalker` or thread-bound context, live in
  `src/jvmUnitTest/kotlin`, and in `src/unitTest/kotlin` of the JVM-only modules. A `fixture` folder holds at least two
  files.
- Virtual time through `runTest` and the `TestClock` of `kore-testing`.

## Build

- Every module applies exactly one plugin, `kheck.kotlin-library`, `kheck.jvm-library`, `kheck.bom` or
  `kheck.benchmark`, and declares nothing but its description and its dependencies.
- Dependencies are declared in `gradle/libs.versions.toml`. JetBrains libraries and the Davils libraries Kore,
  kollect, konfetti, kontinuity, krypt and knot may be added when needed; every other dependency, such as
  `slf4j-api`, needs the approval of a maintainer. None of them may depend on kheck.
- After a dependency changes, refresh the locks and the verification metadata and review what changed:

  ```bash
  ./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --no-configuration-cache
  ```

  ```bash
  ./gradlew --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache check
  ```

- An `ignore` rule of the assurance gate has a reason, the exact module and an expiry no more than six months ahead.

## Kore internals

kheck opts into these `@DavilsInternal` declarations of Kore: `SafeText.text`, `SafeText.quoted`, `SafeText.isSafe`,
`SafeText.REDACTED` and `immutableList`. The host system comes from knot, no longer from `HostPlatform` and
`OperatingSystem` of Kore. A new use is added here and to the table of Kore's `AGENTS.md`.

## Public API

The `api/*.api` dumps are the contract. A deliberate change is recorded with `./gradlew kreateApiDump` (and
`updateKotlinAbi` for the multiplatform modules) and gets a `CHANGELOG.md` entry. A breaking change needs a major
version.

## Releases

The version lives in `gradle.properties`. Every build without a release tag is a `-SNAPSHOT`; a release is a tag
without a `v` on a commit of `main`, published to `https://repo.davils.com`, never to Maven Central.

## Git

Work on `develop`, release from `main`. Conventional Commits in English with the module as scope. Small commits, one
concern each. Commit only when asked. No `Co-Authored-By` trailer and no other attribution to tools or AI assistants.
