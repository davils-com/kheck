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

package com.davils.kheck.core.format.plain

import com.davils.kheck.core.format.TimestampFormat
import com.davils.kheck.core.format.json.JsonLayout
import com.davils.kheck.core.format.json.json
import com.davils.kheck.core.style.SegmentStyle
import com.davils.kheck.core.style.TextStyle
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.TimeZone

private const val FOREIGN: String = "foreign"

class PlainValuesTest : FunSpec({
    context("themes") {
        test("compare by their styles") {
            val copy = PlainTheme.Default.derived { }

            copy shouldBe PlainTheme.Default
            copy.hashCode() shouldBe PlainTheme.Default.hashCode()
            PlainTheme.Default shouldNotBe PlainTheme.Muted
            PlainTheme.Default.equals("theme") shouldBe false
        }

        test("derive a theme that changes only what the block sets") {
            val bold = SegmentStyle(TextStyle(isBold = true))

            val derived = PlainTheme.Default.derived { message = bold }

            derived.message shouldBe bold
            derived.level shouldBe PlainTheme.Default.level
            derived.cause shouldBe PlainTheme.Default.cause
            PlainTheme.Default.message shouldNotBe bold
        }
    }

    context("formats") {
        test("compare plain formats by theme, zone and segments") {
            val first = plain {
                zone = TimeZone.UTC
                layout {
                    timestamp()
                    level()
                    logger()
                    message()
                    fields()
                    cause()
                }
            }
            val second = plain {
                zone = TimeZone.UTC
                layout {
                    timestamp()
                    level()
                    logger()
                    message()
                    fields()
                    cause()
                }
            }

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe plain { zone = TimeZone.UTC }
            first.segments.map { segment -> segment.toString() }.first() shouldContain "Timestamp("
        }

        test("compare segments by their properties") {
            val segments = plain {
                layout {
                    text("|")
                    thread()
                    caller()
                    markers()
                    sequence()
                    context()
                }
            }.segments
            val again = plain {
                layout {
                    text("|")
                    thread()
                    caller()
                    markers()
                    sequence()
                    context()
                }
            }.segments

            segments shouldBe again
            segments.map(PlainSegment::hashCode) shouldBe again.map(PlainSegment::hashCode)
            segments.first().toString() shouldContain "value=\"|\""
            segments.zipWithNext().all { (left, right) -> left != right } shouldBe true
        }

        test("compare JSON formats by their options") {
            json { layout = JsonLayout.Ecs } shouldBe json { layout = JsonLayout.Ecs }
            json { layout = JsonLayout.Ecs }.hashCode() shouldBe json { layout = JsonLayout.Ecs }.hashCode()
            json { layout = JsonLayout.Ecs } shouldNotBe json { layout = JsonLayout.Logstash }
        }
    }

    context("foreign values") {
        test("are never equal to a value of another type") {
            val format = plain { layout { cause() } }
            val layout = (format.segments.single() as PlainSegment.Cause).layout
            val values = listOf(
                format,
                json(),
                layout,
                layout.styles,
                PlainTheme.Default.entries,
                EntriesLayout(affix = SegmentAffix.NONE, separator = " ", styles = EntryStyles()),
            )

            values.forEach { value -> value.equals(FOREIGN) shouldBe false }
        }

        test("tell segments of one type with other properties apart and show them") {
            val first = plain {
                layout {
                    text("a")
                    timestamp()
                    level()
                    thread()
                    logger()
                    caller()
                    markers()
                    message()
                    sequence()
                    fields()
                    context()
                    cause()
                }
            }.segments
            val second = plain {
                layout {
                    text("b")
                    timestamp(TimestampFormat.Time)
                    level { minWidth = 1 }
                    thread { minWidth = 1 }
                    logger(abbreviation = 1)
                    caller { prefix = "@" }
                    markers { prefix = "#" }
                    message { minWidth = 1 }
                    sequence { minWidth = 1 }
                    fields { separator = ";" }
                    context { separator = ";" }
                    cause { maxFrames = 1 }
                }
            }.segments

            first.zip(second).forEach { (left, right) ->
                (left == right) shouldBe false
                left.equals(FOREIGN) shouldBe false
                left.toString().isNotEmpty() shouldBe true
            }
        }
    }
})
