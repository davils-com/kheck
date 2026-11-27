# kheck

kheck is the logging library of Davils: typed records, routing through a hierarchy of loggers, masking of secrets and
personal data, deterministic sampling, deduplication and rate limits, a plain layout with ANSI themes, JSON layouts
for the Elastic Common Schema, Logstash and OpenTelemetry, bounded queues with overflow policies, resilient sinks with
retries, circuit breakers, time limiters and fallbacks, tamper-evident integrity chains, statistics, meters, health and
events, and bridges for SLF4J 2 and `java.util.logging`, for Kotlin on the JVM and on WebAssembly (`wasmJs`). It is
built on [Kore](https://github.com/davils-com/kore) and the Davils libraries
[kontinuity](https://github.com/davils-com/kontinuity), [krypt](https://github.com/davils-com/krypt),
[knot](https://github.com/davils-com/knot), [konfetti](https://github.com/davils-com/konfetti) and
[kollect](https://github.com/davils-com/kollect), and published under the [Apache License 2.0](LICENSE). kheck writes
no file and opens no connection: a sink of the application decides where records go.

| Module | Artifact | What it holds |
|---|---|---|
| `kheck-api` | `com.davils:kheck-api` | What a library needs to log: `Logger`, `LogLevel`, `LoggerName` and the typed events, fields, markers and values |
| `kheck-core` | `com.davils:kheck-core` | The engine: configuration, routing, filters, enrichment, masking, sampling, the plain and JSON layouts, the console and custom sinks, resilience and integrity chains of sinks, statistics, meters, health and events |
| `kheck-slf4j` | `com.davils:kheck-slf4j` | The SLF4J 2 provider: loggers, MDC, markers and key value pairs routed into kheck; JVM only |
| `kheck-jul` | `com.davils:kheck-jul` | The bridge of `java.util.logging` with the levels of kheck mirrored into JUL; JVM only |
| `kheck-knot` | `com.davils:kheck-knot` | The logging closed as the last task of a [knot](https://github.com/davils-com/knot) shutdown and the unexpected failures of knot written as records |
| `kheck-testing` | `com.davils:kheck-testing` | Test support: recording loggers, capturing sinks, a fake terminal, record builders, recorded diagnostics and the contract of a sink |
| `kheck-bom` | `com.davils:kheck-bom` | One version for every module |

kheck requires Kotlin 2.4 or newer and, on the JVM, JDK 25. On Wasm it is tested under Node.js.

## Installation

kheck is published to the Davils repository, not to Maven Central:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://repo.davils.com/releases") }
            filter { includeGroupAndSubgroups("com.davils") }
        }
        mavenCentral()
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(platform("com.davils:kheck-bom:1.0.0"))
    implementation("com.davils:kheck-core")
    runtimeOnly("com.davils:kheck-slf4j")
    testImplementation("com.davils:kheck-testing")
}
```

A library that only logs depends on `kheck-api` alone and takes a `Logger` from its caller.

## A short tour

```kotlin
val logging = logging(applicationScope, Dispatchers.Default) {
    level = LogLevel.Info
    console { format = plain { theme = PlainTheme.Default } }
    console("json") {
        target = ConsoleTarget.StandardError
        format = json { layout = JsonLayout.Ecs }
        markers = setOf(LogMarker.AUDIT)
        isExclusive = true
    }
    logger("com.example.audit") { sinks("json") }
    logger("com.example.db") { level = LogLevel.Warn }
    mask { rules(MaskingRules.PERSONAL_DATA) }
    sample {
        dedupe(window = 10.seconds)
        limit(records = 100, per = 1.seconds, key = RateLimitKey.PerLogger)
    }
    enrich {
        field(name = "service", value = "billing")
        hostname()
    }
}

val logger = logging.logger<InvoiceService>()

logger.info {
    field(name = "invoice", value = invoice.id)
    duration("took", elapsed)
    "Invoice sent"
}

withLogContext({ field(name = "tenant", value = tenant) }) {
    logger.warn {
        cause(failure)
        marker(LogMarker.AUDIT)
        "Payment refused"
    }
}

logging.levels.set("com.example.db", LogLevel.Debug)
logging.statistics()
logging.health()
logging.close(5.seconds)
```

A call of a logger returns at once: the record goes into a bounded intake, and the coroutine of the logging filters,
samples, enriches and masks it before each sink writes it on a coroutine of its own. The block of a disabled level is
never run.

## Resilience, integrity and operations

```kotlin
val environment = environment { timeSource = TimeSource.Monotonic }
val breaker = circuitBreaker("audit.breaker", environment) { waitInOpenState = fixed(30.seconds) }
val limiter = timeLimiter("audit.limiter", environment) { timeout = 2.seconds }

val logging = logging(applicationScope, Dispatchers.Default) {
    sink("audit", collector) {
        stages(breaker, limiter)
        integrity {
            key = auditKey
            anchor { store.lastSeal() }
        }
        fallback = "console"
    }
    console()
    events(bus)
}
val meters = logging.bindTo(registry, labels = mapOf("service" to "billing"))
val health = logging.healthIndicator()
```

- **Resilience** comes from [kontinuity](https://github.com/davils-com/kontinuity): every stage a sink is given, such
  as a retry, a circuit breaker, a time limiter or a whole `resilience { }` policy, wraps every write. A refusal, such
  as an open breaker, sends the batch straight to the fallback and counts it as rejected.
- **Integrity chains** seal every line of a sink with an HMAC over the previous seal and the line. The sink receives
  the seal in `LogLine.seal` and stores it with the line, inline or as the lines of `ChainLines` next to its log;
  `IntegrityChain.verify` proves the log intact or names the first line that was changed, removed, added or moved. The
  chain shows tampering; it prevents neither tampering nor loss.
- **Cryptography** comes from [krypt](https://github.com/davils-com/krypt) under the policy of `crypto`; a hash mask or
  a chain whose algorithm the policy refuses is refused with the configuration.
- **Events** go to the listeners of `onEvent { }` and, with `events(bus)`, to the topic `kheck.logging` of a
  [konfetti](https://github.com/davils-com/konfetti) bus. **Meters** and the **health indicator** are those of
  [kollect](https://github.com/davils-com/kollect).
- **Shutdown**: `close(timeout)` gives up on sinks that hang and counts what was lost. With `kheck-knot`, the logging
  closes as the last task of a knot shutdown, after every other task has logged:

  ```kotlin
  val shutdown = shutdown(applicationScope) {
      logging(logging, timeout = 5.seconds)
      release { task("slf4j-detach") { action { Slf4jBridge.detach() } } }
  }
  ```

- **Own sinks** implement `LogSink` and can check themselves with `LogSinkContract.verify { MySink() }` of
  kheck-testing; `LoggingBuilder.register` gives them a typed DSL of their own.

## What kheck leaves out

| Left out | Why |
|---|---|
| File sinks, rotation, retention, compression | kheck touches no file system; a sink of the application stores records and, with `ChainLines`, their seals |
| Configuration from files | The configuration is a validated DSL; reading a file is the application's |
| Secrets with a value in a record | `secret(name)` holds no value, so no sink can leak it |
| Random sampling | Sampling is deterministic, and kheck never jitters a backoff |

## Design

- **Nothing of a record can fail the caller.** Fields, markers and nested values beyond the limits of
  `com.davils.kheck.api.Limits` are left out and counted in `omitted`, which every layout writes. Filters, masking,
  listeners, health rules and sinks run contained; their failures are counted and reported to the diagnostics.
- **Every foreign text is escaped where it is written.** The plain layout escapes control, format and bidirectional
  characters as `\n` or `\u{…}`; the JSON layouts escape them as JSON `\u` escapes, so the text stays exact. Long texts
  are shortened and marked.
- **Secrets never reach a sink.** `secret("token")` stores no value at all; the masking replaces sensitive names,
  JWTs, bearer and basic credentials, private keys, cloud and Git tokens, assignments of passwords, and, when enabled,
  card numbers, IBANs and email addresses. A masking key for hashed values is copied and never shown.
- **No thread of its own.** The application hands kheck the scope and the dispatcher; on the JVM the bridges of SLF4J
  and JUL are the only process-wide state, because those frameworks are process-wide.
- **Deterministic sampling.** A rate of 0.25 writes exactly every fourth record; no randomness is involved.

## Platforms

| Feature | JVM | WebAssembly |
|---|---|---|
| Console | `System.out` and `System.err`, colors detected from the terminal and the environment | `console.log` and `console.error`, no colors by default |
| Context of coroutines (`withLogContext`, MDC) | follows the coroutine across threads | not visible to a call of a logger; pass fields instead |
| Callers and stack frames | located with `StackWalker` | not available |
| `LogOverflow.Block` on the intake | blocks the caller up to the wait | drops the record, a single thread cannot wait |
| Named time zones of the plain layout | from the JDK | need the time zone database of `kotlinx-datetime` |
| Guard against a sink that logs while it writes | thread-local | none |
| Names of anonymous and local classes (`LoggerName.of`) | the binary name, such as `Service$1` | the simple name or the root |
| Hash mask and integrity chains | JCA through krypt | WebCrypto through krypt, which a browser offers only in a secure context |
| Windows hosts for the colors of the console | detected by knot | detected by knot, also under Node.js |

## Building

```bash
./gradlew check
```

See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md) for the rules of the code base and
[SECURITY.md](SECURITY.md) for reporting a vulnerability.
