# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## 1.0.0 - 2026-11-24

The first version of kheck: the logging library of Davils as an open source library under the Apache License 2.0,
for Kotlin on the JVM and on `wasmJs`, built with Kreate 5.4.1 on Kore 2.3.2 and published to
`https://repo.davils.com`. The code comes from the logging of an internal Davils project and is standalone.

### Added

- **The build** with Kreate: one convention plugin per concern, strict locking and verification, Detekt over main
  and test sources, Kover at 97 %, the API and semver check, Trivy, the standards audit and the assurance gate.
- **The API** in `kheck-api`: `Logger`, `LogLevel`, `LoggerName`, typed events with fields, groups, markers, causes
  and secrets that hold no value, and limits that leave out and count what does not fit.
- **The engine** in `kheck-core`: a bounded intake, routing through a hierarchy of loggers with additive sinks,
  overrides of levels at runtime, reconfiguration, filters, enrichment, the log context of coroutines, masking of
  sensitive names and patterns with full, partial, fixed, removing and HMAC-SHA-256 masks, deterministic sampling,
  deduplication and rate limits, and callers located on the JVM.
- **Layouts**: the plain layout with segments, frames, affixes, stack traces and the themes `Default`, `Monochrome`,
  `Muted` and `HighContrast` in ANSI colors from 16 colors to true color, and JSON layouts for kheck, the Elastic
  Common Schema, Logstash and OpenTelemetry; every foreign text escaped where it is written.
- **Sinks**: the console with the detection of colors, sinks of the application, bounded queues with overflow
  policies, batches, retries with backoff and fallbacks.
- **Statistics, health and events** of the logging, without a dependency on a metrics library.
- **Bridges**: the SLF4J 2 provider with MDC, markers and key value pairs, the bridge of `java.util.logging`, and the
  binding to kollect.
- **Test support** in `kheck-testing`.
