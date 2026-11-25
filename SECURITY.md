# Security Policy

## Supported Versions

Security fixes are released for the latest minor version of the current major release.

| Version | Supported |
|---------|-----------|
| 1.0.x   | Yes       |
| < 1.0   | No        |

## Reporting a Vulnerability

**Do not open a public issue for a security problem.**

Report it privately through GitHub's
[private vulnerability reporting](https://github.com/davils-com/kheck/security/advisories/new),
or by email to development@davils.com.

Please include the affected kheck version, module and target (JVM or Wasm), a description of
the impact, the steps to reproduce it, ideally as a Kotlin test, and any mitigation you are aware of. We acknowledge a
report within three working days and agree on a disclosure date with you.

## Threat Model

kheck runs in the process and with the privileges of the application that uses it. It trusts the application, its
configuration and the sinks, filters, listeners and masking rules the application hands it. It assumes that everything
that reaches a record may come from an attacker:

- the **message, the fields and the markers** of an event, the names of loggers and fields and the values of the log
  context and of the MDC,
- the **message and the stack** of a throwable,
- the **environment** that the console reads for colors and the enrichment reads for host and user.

The properties that must hold:

- **No forged lines.** No record can end a line of the plain layout or a JSON object early, add a line, move the
  cursor or control a terminal: every foreign text is escaped where it is written.
- **No secret in a sink.** A value passed as a secret is never stored; values under sensitive names and texts that
  match the masking rules are replaced before any sink sees them. A masking key never appears in output.
- **No denial of service through a record.** Fields, nesting, lists, markers, captured frames and causes and the
  length of written text are bounded; queues are bounded and every loss is counted.
- **No failure of the caller.** A call of a logger never throws because of a record, a filter, the masking, a format or
  a sink.

## Responsibilities of the Application

- **Masking is a safety net, not a guarantee.** The rules find common secrets and, when enabled, personal data; a
  secret in an unknown shape passes. Log secrets with `secret(name)` and keep personal data out of messages.
- **The rules of the application.** A masking pattern of the application must not backtrack without bound; the
  masking limits the text it scans, not the work of a pattern.
- **Sinks of the application** decide where records go and who may read them; kheck neither encrypts nor rotates.
- **Closing.** Records still in the queues are lost when the scope of the logging is cancelled without `close()`.

## What We Consider a Vulnerability

- a record that forges a line, injects a control sequence or escapes its JSON string in a layout of kheck,
- a secret or a masking key that reaches a sink, a `toString`, an event or a diagnostic,
- input within the limits that makes kheck use unbounded memory or CPU, crash or hang,
- a call of a logger that throws because of what was logged.

The following are **not** vulnerabilities in kheck: a secret in a shape no rule knows, a pattern of the application
that backtracks, a vulnerability in the JDK, Node.js, SLF4J or kollect, and records lost because the application did
not close its logging.
