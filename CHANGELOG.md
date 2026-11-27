# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.1 - 2026-11-27

### Changed

- **The review of ECMA-48 is current.** The page of the standard changed its markup; it still names the 5th edition of
  June 1991, so nothing changes for kheck.

## 1.0.0 - 2026-11-27

The pipeline of 1.0.0 stopped at the standards audit and published nothing; 1.0.1 is the first release of kheck.

The first version of kheck: the logging library of Davils as an open source library under the Apache License 2.0, for
Kotlin on the JVM and on `wasmJs`, built with Kreate 5.5.2 and Gradle 9.9.2 on Kore 2.5.0, konfetti 1.1.0, kollect
1.1.0, kontinuity 1.2.0, krypt 1.2.0 and Knot 1.2.0 and published to `https://repo.davils.com`. The code comes from the
logging of an internal Davils project and builds on the Davils libraries that replace its common modules: kontinuity for
the resilience of sinks, krypt for keys and MACs, knot for the environment, the platform and the shutdown, konfetti for
events and kollect for meters and health. kheck writes no file: a sink of the application decides where records go.

### Added

- **The build** with Kreate: one convention plugin per concern, strict locking and verification, Detekt over main
  and test sources, Kover at 97 %, the API and semver check, Trivy, the standards audit and the assurance gate.
- **The API** in `kheck-api`: `Logger`, `LogLevel`, `LoggerName`, typed events with fields, groups, markers, causes
  and secrets that hold no value, and limits that leave out and count what does not fit.
- **The engine** in `kheck-core`: a bounded intake, routing through a hierarchy of loggers with additive sinks,
  overrides of levels at runtime, reconfiguration, filters, enrichment, the log context of coroutines through
  `withLogContext` and `logContext`, masking of sensitive names and patterns with full, partial, fixed, removing and
  hashing masks, deterministic sampling, deduplication and rate limits, and callers located on the JVM. A rate limit
  per key is a keyed rate limiter of kontinuity with a token bucket per key, which refuses the records of a new key
  while every counted key is in use, so a flood of keys cannot lift it; the routes of loggers are kept in a cache of
  kontinuity.
- **Layouts**: the plain layout with segments, frames, affixes, stack traces and the themes `Default`, `Monochrome`,
  `Muted` and `HighContrast` in ANSI colors from 16 colors to true color, changed through `PlainTheme.derived { }`,
  and JSON layouts for kheck, the Elastic Common Schema, Logstash and OpenTelemetry; every foreign text escaped where
  it is written.
- **Sinks**: the console with the detection of colors, sinks of the application through `LogSink` and the extension
  point `LoggingBuilder.register`, bounded queues with overflow policies, batches and fallbacks.
- **Resilience of sinks** with kontinuity: `stages(...)` on every sink takes retries, circuit breakers, time limiters
  or a whole policy; a refusal of a stage, such as an open circuit breaker, hands the batch to the fallback and counts
  it as `rejected`. A time limiter also bounds opening, flushing and closing. A stage given to two sinks is refused
  with `SHARED_STAGE`.
- **Integrity chains** without a file system: `integrity { key; algorithm; anchor { } }` on a sink seals every line
  with a `LogSeal` in `LogLine.seal`, chained by HMAC over the previous seal and the line. `IntegrityChain.verify`
  proves a log intact or names the first tampered line, from lines with seals or from a log and the lines of
  `ChainLines`.
- **Cryptography of krypt**: `LoggingBuilder.crypto` with the modern policy by default, the hash mask keyed by a
  `SecretKey` of krypt in `MaskingBuilder.hashKey` with `MaskingBuilder.algorithm`, such as HMAC-SHA-384 for CNSA 2.0.
  A configuration whose algorithm the policy refuses is refused with `CRYPTO_REFUSED`.
- **Environment of knot**: `LoggingBuilder.environment` and `ConsoleSinkBuilder.environment` are environments of knot;
  the host system comes from knot, which finds Windows under Node.js as well.
- **Statistics, health and events**: statistics per sink with the circuit state, health rules with the codes of
  kollect, `LoggingEvent` as a konfetti event published by `events(bus)` on `LoggingEvent.TOPIC` without waiting.
- **Meters and health of kollect**: `Logging.bindTo(registry, labels)` declares the statistics as meters with a timer
  of the writes of every sink, and `Logging.healthIndicator()` reports the health with its reason codes.
- **Bounded closing**: `Logging.close(timeout)` returns a `Closure` and cancels sinks that hang, counting the records
  that were still waiting; `Logging.flush(timeout)` tells whether the flush ended in time.
- **Bridges**: the SLF4J 2 provider with MDC, markers and key value pairs, and the bridge of `java.util.logging`.
- **`kheck-knot`**: `logging(...)` on a shutdown plan of knot closes the logging as the last task, `Shutdown.adopt`
  joins a running shutdown, and `Logging.failureSink()` writes the unexpected failures of knot as error records.
- **Test support** in `kheck-testing`, with `LogSinkContract`, which checks a sink against the contract of `LogSink`.
- **`kheck-benchmark`**, never published: JMH benchmarks of the disabled call, the pipeline with the hash mask and the
  integrity chain, and the verification of a chain.
