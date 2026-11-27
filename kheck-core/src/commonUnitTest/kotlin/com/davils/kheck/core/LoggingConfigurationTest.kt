/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kheck.core

import com.davils.kheck.api.LogLevel
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.loggingCodes
import com.davils.kheck.core.fixture.loggingConfiguration
import com.davils.kheck.core.fixture.loggingReport
import com.davils.kheck.core.intake.IntakeConfiguration
import com.davils.kheck.core.mask.fixture.secretKey
import com.davils.kheck.core.sink.LogOverflow
import com.davils.kheck.testing.CapturingSink
import com.davils.kore.core.validation.report.ValidationException
import com.davils.krypt.core.algorithm.MacAlgorithm
import com.davils.krypt.core.environment
import com.davils.krypt.core.policy.CryptoPolicy
import com.davils.krypt.primitive.Crypto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration

private const val LOGGER: String = "a"
private const val MISSING: String = "missing"
private const val TWICE: String = "twice"
private const val QUIET: String = "quiet"
private const val CAPTURE: String = "capture"
private const val BACKUP: String = "backup"

class LoggingConfigurationTest : FunSpec({
    context("sinks") {
        test("refuses routes to unknown sinks") {
            loggingCodes { logger(LOGGER) { sinks(MISSING) } } shouldBe listOf(LoggingViolationCodes.UNKNOWN_SINK)
        }

        test("names the unknown sinks it refuses") {
            val report = loggingReport {
                capture(CapturingSink()) { fallback = BACKUP }
                logger(LOGGER) { sinks(MISSING, TWICE) }
            }

            report.errors.map { violation -> violation.message } shouldBe listOf(
                """falls back to "$BACKUP", a sink that is not declared""",
                """routes to sinks that are not declared: "$MISSING", "$TWICE"""",
            )
        }

        test("refuses a hash mask whose algorithm the policy of the cryptography forbids") {
            val cnsa = Crypto(environment { policy = CryptoPolicy.Cnsa2 })

            loggingCodes {
                crypto = cnsa
                mask { hashKey = secretKey("key-0123456789abcdef") }
            } shouldBe listOf(LoggingViolationCodes.CRYPTO_REFUSED)
            loggingCodes {
                crypto = cnsa
                mask {
                    hashKey = secretKey("key-0123456789abcdef")
                    algorithm = MacAlgorithm.HmacSha384
                }
            } shouldBe emptyList()
        }

        test("refuses a fallback to itself or to an unknown sink") {
            loggingCodes {
                capture(CapturingSink(), name = "first") { fallback = "first" }
                capture(CapturingSink(), name = "second") { fallback = MISSING }
            } shouldBe listOf(LoggingViolationCodes.SELF_FALLBACK, LoggingViolationCodes.UNKNOWN_SINK)
        }

        test("refuses a blank name and a sink declared twice") {
            val report = loggingReport {
                capture(CapturingSink(), name = " ")
                capture(CapturingSink(), name = TWICE)
                capture(CapturingSink(), name = TWICE)
            }

            report.errors.size shouldBe 2
        }

        test("refuses sink options without room") {
            loggingCodes {
                capture(CapturingSink()) {
                    capacity = 0
                    maxBatch = 0
                    overflow = LogOverflow.Block(Duration.ZERO)
                }
            } shouldBe listOf(
                LoggingViolationCodes.INVALID_CAPACITY,
                LoggingViolationCodes.INVALID_BATCH,
                LoggingViolationCodes.INVALID_WAIT,
            )
        }

        test("refuses a batch above the capacity") {
            loggingCodes {
                capture(CapturingSink()) {
                    capacity = 1
                    maxBatch = 2
                }
            } shouldBe listOf(LoggingViolationCodes.INVALID_BATCH)
        }

        test("warns about an exclusive sink nobody routes to and about missing sinks") {
            val exclusive = loggingReport { capture(CapturingSink()) { isExclusive = true } }
            val empty = loggingReport { }
            val silent = loggingReport { level = LogLevel.Off }

            exclusive.warnings.single().code shouldBe LoggingViolationCodes.UNROUTED_SINK
            empty.warnings.single().code shouldBe LoggingViolationCodes.NO_SINKS
            silent.violations shouldBe emptyList()
        }

        test("refuses to build an invalid configuration") {
            shouldThrow<ValidationException> { loggingConfiguration { logger(LOGGER) { sinks(MISSING) } } }
        }
    }

    context("intake") {
        test("refuses an intake without room") {
            loggingCodes {
                intake {
                    capacity = 0
                    overflow = LogOverflow.Block(Duration.INFINITE)
                }
            } shouldBe listOf(LoggingViolationCodes.INVALID_CAPACITY, LoggingViolationCodes.INVALID_WAIT)
        }

        test("compares intakes by value") {
            val first = IntakeConfiguration(capacity = 1, overflow = LogOverflow.DropNewest)
            val second = IntakeConfiguration(capacity = 1, overflow = LogOverflow.DropNewest)

            first shouldBe second
        }
    }

    context("description") {
        test("names loggers, sinks and limits") {
            val configuration = loggingConfiguration {
                capture(CapturingSink())
                capture(CapturingSink(), name = BACKUP) { fallback = CAPTURE }
                logger(LOGGER) { level = LogLevel.Debug }
                logger(QUIET) { isCallerIncluded = true }
            }
            val loggers = configuration.loggers
            val sinks = configuration.sinks

            configuration.toString() shouldContain "sinks=[capture, backup]"
            loggers.getValue(LOGGER).toString() shouldContain "level=Debug, isAdditive=true, isCallerIncluded=inherited"
            loggers.getValue(QUIET).toString() shouldContain "level=inherited, isAdditive=true, isCallerIncluded=true"
            sinks.getValue(CAPTURE).toString() shouldContain "capacity=8192"
            sinks.getValue(CAPTURE).toString() shouldContain "fallback=none"
            sinks.getValue(BACKUP).toString() shouldContain "fallback=capture"
        }
    }
})
