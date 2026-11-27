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
- **Integrity chains** prove that the lines a sink stored were not changed, removed, added or moved, for anyone who
  holds the key; they do not prevent it. Whoever holds the key can forge a chain, so the key stays with the
  application, apart from the stored lines. A chain does not show lines lost before they reached the sink, such as by
  an overflow; the notice of dropped records is sealed instead. The chain goes on across restarts only from the anchor
  the application stores.
- **Cryptography** runs through krypt under the policy of the logging; the application chooses the policy, such as
  CNSA 2.0 with HMAC-SHA-384, and destroys its keys when they are no longer needed.
- **Closing.** Records still in the queues are lost when the scope of the logging is cancelled without `close()`.

## What We Consider a Vulnerability

- a record that forges a line, injects a control sequence or escapes its JSON string in a layout of kheck,
- a secret, a masking key or the key of an integrity chain that reaches a sink, a `toString`, an event or a
  diagnostic,
- an integrity chain that verifies as intact although a sealed line was changed, removed, added or moved,
- input within the limits that makes kheck use unbounded memory or CPU, crash or hang,
- a call of a logger that throws because of what was logged.

The following are **not** vulnerabilities in kheck: a secret in a shape no rule knows, a pattern of the application
that backtracks, a vulnerability in the JDK, Node.js, SLF4J or kollect, and records lost because the application did
not close its logging.
