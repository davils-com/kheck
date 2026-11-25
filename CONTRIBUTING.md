# Contributing to kheck

Thank you for helping to build kheck. This document explains how a change gets in. The binding rules for code,
tests and the build are in [AGENTS.md](AGENTS.md); read it before your first change.

## Code of Conduct

Everyone who takes part follows the [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

1. Open an issue for anything larger than a small fix, so the approach can be agreed on first. Security problems are
   never reported as an issue; see [SECURITY.md](SECURITY.md).
2. Branch from `develop` and keep the change to one concern.
3. Run `./gradlew check` until it is green. It needs JDK 25 and network access.
4. Open a pull request against `develop` and describe what changed and why.

## Development Standards

### One command before you push

```bash
./gradlew check
```

It compiles with warnings as errors, runs Detekt, the tests on the JVM and on Wasm, coverage, the API check, the
license header check, the changelog check, Trivy, the standards audit and the Kreate assurance gate. A pull request is
only merged when it is green.

### Safe before convenient

kheck writes text that may come from an attacker into terminals and log pipelines. A change to a layout ships with an
injection test with the hostile fragments of `kore-testing`; a change to the masking ships with a test that the secret
appears nowhere; a new limit ships with a test at the limit and one past it.

## Changing a Dependency

Every dependency, the classpath of `build-logic` included, is locked and verified. After changing
`gradle/libs.versions.toml`, refresh both in one run and review what changed:

```bash
./gradlew kreateResolveAndLockAll :build-logic:dependencies --write-locks --write-verification-metadata pgp,sha256 --export-keys --no-configuration-cache
```

A new dependency other than a library of JetBrains, Kore or kollect needs the approval of a maintainer.
Kore, kollect and Kreate are used in released versions only, never as snapshots.

## Snapshots and Releases

`gradle.properties` holds the version in development. Every push to `develop` publishes it as a `-SNAPSHOT` to
`https://repo.davils.com/snapshots`. A release is the tag of that version without a `v`, such as `1.0.0`, pushed on a
commit of `main`; the pipeline signs it, publishes it to `https://repo.davils.com/releases` and attests it. Before the
tag, the version gets its section in [CHANGELOG.md](CHANGELOG.md).

## Commit Messages

Conventional Commits in English with the module as scope, such as `feat(primitive): add HPKE in PSK mode` or
`fix(asn1): refuse a non-minimal length`. A breaking change is marked with `!` and a `BREAKING CHANGE:`
footer.
