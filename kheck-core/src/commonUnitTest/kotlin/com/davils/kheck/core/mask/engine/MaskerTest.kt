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

package com.davils.kheck.core.mask.engine

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.mask.Mask
import com.davils.kheck.core.mask.MaskingBuilder
import com.davils.kheck.core.mask.MaskingConfiguration
import com.davils.kheck.core.mask.MaskingKey
import com.davils.kheck.core.mask.fixture.CountingMasker
import com.davils.kheck.core.record.LogCause
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val SHORT_LIMIT: Int = 256
private const val LONG_TEXT: Int = 300
private const val HASH_LENGTH: Int = 21
private const val KEYS: Int = 4_100
private const val PASSWORD: String = "password"
private const val HUNTER: String = "hunter2"
private const val USER: String = "user"
private const val ALICE: String = "alice"
private const val MASK: String = "****"
private const val ASSIGNMENT: String = "secret-assignment"
private const val KEY: String = "key"

class MaskerTest : FunSpec({
    context("records") {
        test("masks message, fields, context and causes in one pass") {
            val counting = CountingMasker { }
            val record = logRecord {
                message = "login with password=$HUNTER"
                fields {
                    field(name = PASSWORD, value = HUNTER)
                    field(name = USER, value = ALICE)
                    group("database") { field(name = "dbPassword", value = "secret") }
                    secret("session")
                    field("list", LogValue.Items(listOf(LogValue.Text("token=xyz"), LogValue.Integer(1L))))
                }
                context { field(name = "authorization", value = "Bearer abcdefgh12345678") }
            }.copied(
                cause = LogCause(
                    type = "Failure",
                    message = "password=one",
                    cause = LogCause(type = "Inner", message = "secret=two"),
                    suppressed = listOf(LogCause(type = "Closing", message = null)),
                ),
            )

            val masked = counting.masker.record(record)

            masked.message shouldBe "login with password=$MASK"
            masked.fields shouldBe mapOf(
                PASSWORD to LogValue.Text(MASK),
                USER to LogValue.Text(ALICE),
                "database" to LogValue.Entries(mapOf("dbPassword" to LogValue.Text(MASK))),
                "session" to LogValue.Secret,
                "list" to LogValue.Items(listOf(LogValue.Text("token=$MASK"), LogValue.Integer(1L))),
            )
            masked.context shouldBe mapOf("authorization" to LogValue.Text(MASK))
            masked.cause?.message shouldBe "password=$MASK"
            masked.cause?.cause?.message shouldBe "secret=$MASK"
            counting.rules shouldBe listOf(ASSIGNMENT, KEY, KEY, "secret", ASSIGNMENT, KEY, ASSIGNMENT, ASSIGNMENT)
        }

        test("hides values below the depth limit") {
            val counting = CountingMasker { maxDepth = 1 }
            val record = logRecord {
                fields {
                    group("outer") { group("inner") { field("value", 1) } }
                    field("items", LogValue.Items(listOf(LogValue.Items(emptyList()))))
                }
            }

            counting.masker.record(record).fields shouldBe mapOf(
                "outer" to LogValue.Entries(mapOf("inner" to LogValue.Text(MASK))),
                "items" to LogValue.Items(listOf(LogValue.Text(MASK))),
            )
        }

        test("keeps values that cannot hold a secret") {
            val values = listOf(
                LogValue.Integer(1L),
                LogValue.Decimal(1.5),
                LogValue.Bool(true),
                LogValue.Timestamp(Instant.parse("2026-01-01T00:00:00Z")),
                LogValue.Elapsed(1.seconds),
                LogValue.Null,
            )
            val record = logRecord { fields { values.forEachIndexed { index, value -> field("value$index", value) } } }

            maskedFields(record) { }.values.toList() shouldBe values
        }

        test("remembers its decision about a key") {
            val counting = CountingMasker { }
            val record = logRecord { fields { field(name = PASSWORD, value = HUNTER) } }

            counting.masker.record(record)
            counting.masker.record(record)

            counting.rules shouldBe listOf(KEY, KEY)
        }

        test("decides about a key once and starts over when too many keys were seen") {
            val counting = CountingMasker { }
            val record = logRecord().copied(fields = (0..KEYS).associate { index -> "key$index" to LogValue.Text("v") })

            counting.masker.record(record)
            val masked = counting.masker.record(record)

            masked.fields.size shouldBe KEYS + 1
        }

        test("falls back to a full mask when a hash has no key") {
            val masker = Masker(MaskingConfiguration(MaskingBuilder().apply { default = Mask.Hash })) { }
            val record = logRecord { fields { field(name = PASSWORD, value = HUNTER) } }

            masker.record(record).fields[PASSWORD] shouldBe LogValue.Text(MASK)
        }

        test("cuts values above the length limit") {
            val counting = CountingMasker { maxValueLength = SHORT_LIMIT }

            counting.text("a".repeat(LONG_TEXT)) shouldEndWith "…(+44 chars)"
        }
    }

    context("masks of secret keys") {
        test("keeps the start and end of a long value") {
            maskedKey("hunter2secret") { default = Mask.Partial(keepStart = 2, keepEnd = 2) } shouldBe
                LogValue.Text("hu****et")
        }

        test("hides a value that is too short to keep a part of") {
            maskedKey(
                "short",
            ) { default = Mask.Partial(keepStart = 2, keepEnd = 2) } shouldBe LogValue.Text(MASK)
        }

        test("replaces a value with a keyed hash that stays the same") {
            val first = maskedKey(HUNTER) { hashKeyed() }
            val second = maskedKey(HUNTER) { hashKeyed() }

            first shouldBe second
            val hash = first.shouldBeInstanceOf<LogValue.Text>().value
            hash shouldStartWith "hash:"
            hash shouldHaveLength HASH_LENGTH
        }

        test("replaces a value with a fixed text") {
            maskedKey(HUNTER) { default = Mask.Fixed("[redacted]") } shouldBe LogValue.Text("[redacted]")
        }

        test("removes the field and blanks inline matches") {
            val counting = CountingMasker { default = Mask.Remove }
            val record = logRecord {
                message = "password=$HUNTER"
                fields {
                    field(name = PASSWORD, value = HUNTER)
                    field(name = USER, value = ALICE)
                }
            }

            val masked = counting.masker.record(record)

            masked.fields shouldNotContainKey PASSWORD
            masked.fields shouldContainKey USER
            masked.message shouldBe "password="
        }

        test("recognises keys by pattern and spelling") {
            val record = logRecord {
                fields {
                    field(name = "CUSTOMER_NUMBER", value = "c-1")
                    field(name = "x-api-key", value = "k")
                    field(name = "smtpPasswordHash", value = "h")
                    field(name = "host", value = "db")
                }
            }

            maskedFields(record) { keys("customerNumber") }.values.toList() shouldBe listOf(
                LogValue.Text(MASK),
                LogValue.Text(MASK),
                LogValue.Text(MASK),
                LogValue.Text("db"),
            )
        }
    }
})

private fun MaskingBuilder.hashKeyed() {
    default = Mask.Hash
    hashKey = MaskingKey.of("audit-key-0123456789")
}

private fun maskedFields(record: LogRecord, block: MaskingBuilder.() -> Unit): Map<String, LogValue> {
    val masked = CountingMasker(block).masker.record(record)
    return masked.fields
}

private fun maskedKey(value: String, block: MaskingBuilder.() -> Unit): LogValue? {
    val record = logRecord { fields { field(PASSWORD, value) } }
    return maskedFields(record, block)[PASSWORD]
}
