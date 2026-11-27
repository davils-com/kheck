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

package com.davils.kheck.core.integrity

import com.davils.kheck.api.info
import com.davils.kheck.core.Logging
import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kheck.core.fixture.SERVICE
import com.davils.kheck.core.fixture.capture
import com.davils.kheck.core.fixture.loggingCodes
import com.davils.kheck.core.fixture.recorded
import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.mask.fixture.secretKey
import com.davils.kheck.testing.CapturingSink
import com.davils.krypt.core.algorithm.MacAlgorithm
import com.davils.krypt.core.environment
import com.davils.krypt.core.policy.CryptoPolicy
import com.davils.krypt.primitive.Crypto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString

private const val KEY: String = "chain-key-0123456789"
private const val FALLBACK: String = "fallback"
private val CRYPTO: Crypto = Crypto()
private val ORIGIN: LogSeal = LogSeal.origin(MacAlgorithm.HmacSha256)
private const val LINES: Long = 3L
private const val TOO_MANY_BYTES: Int = 65

class IntegrityChainTest : FunSpec({
    context("sealing") {
        test("seals every line in order and verifies the chain") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) { integrity { key = secretKey(KEY) } } }

                logging.written()
                logging.flush()

                sink.lines.map { line -> line.seal?.sequence } shouldBe (1L..LINES).toList()
                verified(sink.sealed()) shouldBe IntegrityReport.Intact(
                    records = 3L,
                    anchor = ORIGIN,
                    last = sink.lastSeal(),
                )
            }
        }

        test("goes on from the anchor of an earlier chain") {
            runTest {
                val earlier = CapturingSink()
                val first = testLogging { capture(earlier) { integrity { key = secretKey(KEY) } } }
                first.logger(SERVICE).info { "before" }
                first.flush()
                val anchor = earlier.lastSeal()
                val later = CapturingSink()
                val second = testLogging {
                    capture(later) {
                        integrity {
                            key = secretKey(KEY)
                            anchor { anchor }
                        }
                    }
                }

                second.logger(SERVICE).info { "after" }
                second.flush()

                later.onlySequence() shouldBe 2L
                verified(later.sealed(), anchor) shouldBe IntegrityReport.Intact(
                    records = 1L,
                    anchor = anchor,
                    last = later.lastSeal(),
                )
            }
        }

        test("starts at the origin and reports when the anchor fails") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging {
                    capture(sink) {
                        integrity {
                            key = secretKey(KEY)
                            anchor { error("unreadable") }
                        }
                    }
                }

                logging.logger(SERVICE).info { "first" }
                logging.flush()

                sink.onlySequence() shouldBe 1L
                logging.recorded.messages.single() shouldContain "anchor failed"
            }
        }

        test("leaves no gap when a batch fails and keeps a chain of its own in the fallback") {
            runTest {
                val primary = CapturingSink()
                val fallback = CapturingSink()
                val logging = testLogging {
                    capture(primary) {
                        this.fallback = FALLBACK
                        integrity { key = secretKey(KEY) }
                    }
                    capture(fallback, name = FALLBACK) {
                        isExclusive = true
                        integrity { key = secretKey(KEY) }
                    }
                }
                primary.fail(IllegalStateException("down"))
                logging.logger(SERVICE).info { "lost" }
                logging.flush()
                primary.recover()

                logging.logger(SERVICE).info { "kept" }
                logging.flush()

                primary.onlySequence() shouldBe 1L
                fallback.onlySequence() shouldBe 1L
                verified(primary.sealed()).shouldBeIntact()
                verified(fallback.sealed()).shouldBeIntact()
            }
        }

        test("fails a batch it cannot seal and hands it to the fallback") {
            runTest {
                val key = secretKey(KEY)
                val primary = CapturingSink()
                val fallback = CapturingSink()
                val logging = testLogging {
                    capture(primary) {
                        this.fallback = FALLBACK
                        integrity { this.key = key }
                    }
                    capture(fallback, name = FALLBACK) { isExclusive = true }
                }
                key.destroy()

                logging.logger(SERVICE).info { "unsealed" }
                logging.flush()

                primary.lines shouldBe emptyList()
                fallback.messages shouldBe listOf("unsealed")
            }
        }
    }

    context("tampering") {
        test("finds a changed, a removed and a moved line") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) { integrity { key = secretKey(KEY) } } }
                logging.written()
                logging.flush()
                val entries = sink.sealed()

                val changed = entries.mapIndexed { index, entry ->
                    SealedText(text = entry.text.takeUnless { index == 1 } ?: "forged\n", seal = entry.seal)
                }
                verified(changed) shouldBe IntegrityReport.Tampered(2L)
                verified(entries.filterIndexed { index, _ -> index != 1 }) shouldBe IntegrityReport.Tampered(2L)
                verified(entries.reversed()) shouldBe IntegrityReport.Tampered(1L)
            }
        }

        test("checks a log against the lines of its chain") {
            runTest {
                val sink = CapturingSink()
                val logging = testLogging { capture(sink) { integrity { key = secretKey(KEY) } } }
                logging.written()
                logging.flush()
                val entries = sink.sealed()
                val log = entries.joinToString(separator = "") { entry -> entry.text }.encodeToByteArray()
                val chain = listOf(ChainLines.anchor(ORIGIN)) +
                    entries.map { entry -> ChainLines.format(entry.seal, entry.text) }

                stored(log, chain).shouldBeIntact()
                stored(log.copyOf(log.size - 1), chain) shouldBe IntegrityReport.Incomplete(2L)
                stored(log + "extra".encodeToByteArray(), chain) shouldBe IntegrityReport.Incomplete(LINES)
                stored(log.copyOf().also { bytes -> bytes[0] = 'X'.code.toByte() }, chain) shouldBe
                    IntegrityReport.Tampered(1L)
                stored(log, chain.filterIndexed { index, _ -> index != 2 }) shouldBe IntegrityReport.Tampered(2L)
                stored(log, listOf(chain[0], "1 two ab")) shouldBe IntegrityReport.Malformed(2)
                stored(log, emptyList()) shouldBe IntegrityReport.Malformed(1)
                val tooLong = "1 0 " + "ab".repeat(TOO_MANY_BYTES)
                val broken = listOf("1 2", "-1 0 ab", "1 -2 ab", "1 two ab", "1 0 zz", "1 0 abc", tooLong)
                val reports = broken.map { line -> stored(log, listOf(chain[0], line)) }
                reports.toSet() shouldBe setOf(IntegrityReport.Malformed(2))
            }
        }
    }

    context("validation") {
        test("refuses a chain without a key or with a destroyed key") {
            val destroyed = secretKey(KEY).also { key -> key.destroy() }

            loggingCodes { capture(CapturingSink()) { integrity { } } } shouldBe
                listOf(LoggingViolationCodes.MISSING_INTEGRITY_KEY)
            loggingCodes { capture(CapturingSink()) { integrity { key = destroyed } } } shouldBe
                listOf(LoggingViolationCodes.MISSING_INTEGRITY_KEY)
        }

        test("refuses an algorithm the policy of the cryptography forbids") {
            loggingCodes {
                crypto = Crypto(environment { policy = CryptoPolicy.Cnsa2 })
                capture(CapturingSink()) { integrity { key = secretKey(KEY) } }
            } shouldBe listOf(LoggingViolationCodes.CRYPTO_REFUSED)
        }
    }
})

private fun CapturingSink.sealed(): List<SealedText> =
    lines.map { line -> SealedText(text = line.text, seal = line.seal.shouldNotBeNull()) }

private suspend fun verified(entries: List<SealedText>, anchor: LogSeal = ORIGIN): IntegrityReport =
    IntegrityChain.verify(
        crypto = CRYPTO,
        key = secretKey(KEY),
        algorithm = MacAlgorithm.HmacSha256,
        anchor = anchor,
        entries = entries.asFlow(),
    )

private suspend fun stored(log: ByteArray, chain: List<String>): IntegrityReport = IntegrityChain.verify(
    crypto = CRYPTO,
    key = secretKey(KEY),
    algorithm = MacAlgorithm.HmacSha256,
    log = ByteString(log),
    chain = chain,
)

private fun IntegrityReport.shouldBeIntact() {
    (this is IntegrityReport.Intact) shouldBe true
}

private fun Logging.written() {
    val logger = logger(SERVICE)
    repeat(LINES.toInt()) { index -> logger.info { "line $index" } }
}

private fun CapturingSink.lastSeal(): LogSeal = lines.last().seal.shouldNotBeNull()

private fun CapturingSink.onlySequence(): Long? = lines.single().seal?.sequence
