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

package com.davils.kheck.api.record

import com.davils.kheck.api.Limits
import com.davils.kore.core.annotation.DavilsInternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val LONG: Long = 7L
private const val DECIMAL: Double = 0.5
private const val ENTRIES: String = "entries"
private const val LOST_ITEMS: Int = 3
private const val LOST_ENTRIES: Int = 2

@OptIn(DavilsInternal::class)
class LogFieldsBuilderTest : FunSpec({
    context("fields") {
        test("type every value") {
            val instant = Instant.parse("2026-01-01T00:00:00Z")
            val builder = LogEventBuilder()
            builder.field(name = "text", value = "a")
            builder.field("int", 1)
            builder.field("long", LONG)
            builder.field("decimal", DECIMAL)
            builder.field("flag", false)
            builder.timestamp("at", instant)
            builder.duration("took", 2.seconds)
            builder.secret("key")
            builder.marker(LogMarker.SECURITY)
            val event = builder.build("message")
            event.fields shouldBe mapOf(
                "text" to LogValue.Text("a"),
                "int" to LogValue.Integer(1L),
                "long" to LogValue.Integer(LONG),
                "decimal" to LogValue.Decimal(DECIMAL),
                "flag" to LogValue.Bool(false),
                "at" to LogValue.Timestamp(instant),
                "took" to LogValue.Elapsed(2.seconds),
                "key" to LogValue.Secret,
            )
            event.markers shouldBe setOf(LogMarker.SECURITY)
            event.cause shouldBe null
            event.omitted shouldBe 0
        }

        test("write a missing text as null and a group as nested entries") {
            val builder = LogFieldsBuilder()
            builder.field(name = "owner", value = null as String?)
            builder.group("peer") {
                field("port", 1)
            }
            builder.field("tags", LogValue.Items(listOf(LogValue.Text("a"))))
            builder.collected() shouldBe mapOf(
                "owner" to LogValue.Null,
                "peer" to LogValue.Entries(mapOf("port" to LogValue.Integer(1L))),
                "tags" to LogValue.Items(listOf(LogValue.Text("a"))),
            )
            LogFieldsBuilder().collected() shouldBe emptyMap()
        }

        test("keep the last value of a repeated name") {
            val builder = LogFieldsBuilder()
            builder.field(name = "name", value = "first")
            builder.field(name = "name", value = "second")
            builder.collected() shouldBe mapOf("name" to LogValue.Text("second"))
        }
    }

    context("limits") {
        test("leave out and count the fields beyond the most a record holds") {
            val builder = LogFieldsBuilder()
            repeat(Limits.MAX_FIELDS + 2) { index -> builder.field("f$index", index) }
            builder.field(name = "f0", value = "replaced")
            builder.collected().size shouldBe Limits.MAX_FIELDS
            builder.collected()["f0"] shouldBe LogValue.Text("replaced")
            builder.omitted shouldBe 2
        }

        test("leave out and count a field whose name is too long") {
            val builder = LogFieldsBuilder()
            builder.field("n".repeat(Limits.MAX_NAME_LENGTH + 1), 1)
            builder.collected() shouldBe emptyMap()
            builder.omitted shouldBe 1
        }

        test("leave out and count the groups nested deeper than the limit") {
            val builder = LogFieldsBuilder()

            fun LogFieldsBuilder.nest(level: Int) {
                if (level == 0) {
                    field("leaf", true)
                    return
                }
                group("g") { nest(level - 1) }
            }
            builder.nest(Limits.MAX_DEPTH + 2)
            builder.omitted shouldBe 1
        }

        test("cut lists and entries that exceed the limits and count what they lose") {
            val builder = LogFieldsBuilder()
            val many = List(Limits.MAX_ITEMS + LOST_ITEMS) { index -> LogValue.Integer(index.toLong()) }
            builder.field("items", LogValue.Items(many))
            val wide = (0..Limits.MAX_FIELDS).associate { index -> "k$index" to LogValue.Bool(true) }
            builder.field(ENTRIES, LogValue.Entries(wide + ("n".repeat(Limits.MAX_NAME_LENGTH + 1) to LogValue.Null)))
            val items = builder.collected().getValue("items") as LogValue.Items
            items.values.size shouldBe Limits.MAX_ITEMS
            val entries = builder.collected().getValue(ENTRIES) as LogValue.Entries
            entries.values.size shouldBe Limits.MAX_FIELDS
            builder.omitted shouldBe LOST_ITEMS + LOST_ENTRIES
        }

        test("leave out and count an entry whose name is too long") {
            val builder = LogFieldsBuilder()
            val long = "n".repeat(Limits.MAX_NAME_LENGTH + 1)
            builder.field(ENTRIES, LogValue.Entries(mapOf(long to LogValue.Null, "short" to LogValue.Null)))
            (builder.collected().getValue(ENTRIES) as LogValue.Entries).values.keys shouldBe setOf("short")
            builder.omitted shouldBe 1
        }

        test("replace a value nested deeper than the limit by null and count it") {
            var value: LogValue = LogValue.Text("deep")
            repeat(Limits.MAX_DEPTH) { index ->
                val inner = value
                value = LogValue.Items(listOf(LogValue.Entries(mapOf("e$index" to inner))))
            }
            val builder = LogFieldsBuilder()
            builder.field("deep", value)
            builder.omitted shouldBe 1
        }

        test("replace a list nested deeper than the limit by null and count it") {
            var value: LogValue = LogValue.Null
            repeat(Limits.MAX_DEPTH + 1) { value = LogValue.Items(listOf(value)) }
            val builder = LogFieldsBuilder()
            builder.field(name = "deep", value = value)
            builder.omitted shouldBe 1
        }

        test("leave out and count the markers beyond the most an event carries") {
            val builder = LogEventBuilder()
            repeat(Limits.MAX_MARKERS + 1) { index -> builder.marker(LogMarker("m$index")) }
            builder.marker(LogMarker("m0"))
            val event = builder.build("message")
            event.markers.size shouldBe Limits.MAX_MARKERS
            event.omitted shouldBe 1
        }
    }
})
