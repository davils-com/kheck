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

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.core.format.TimestampFormat
import com.davils.kheck.core.format.fixture.FAILURE
import com.davils.kheck.core.format.fixture.rendered
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.testing.logRecord
import com.davils.kore.core.validation.report.ValidationReport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset

private const val HEADER: String = "2026-11-10 08:15:30.250 INFO  [main] com.example.inventory.ServerService - "
private const val ABBREVIATION: Int = 20
private const val COUNT: Long = 3L
private const val INVALID_SEGMENTS: Int = 6

class PlainFormatTest : FunSpec({
    val format = plain { zone = TimeZone.UTC }

    context("default layout") {
        test("renders timestamp, level, thread, logger and message on one line") {
            format.rendered(logRecord()) shouldBe "${HEADER}loaded the servers\n"
        }

        test("appends fields and context as key value pairs") {
            val record = logRecord {
                fields {
                    field("count", COUNT)
                    field(name = "name", value = "rack one")
                }
                context { field(name = "tenant", value = "acme") }
            }

            format.rendered(
                record,
            ) shouldBe HEADER + """loaded the servers count=3 name="rack one" tenant=acme""" + "\n"
        }

        test("puts the markers in front of the message") {
            val record = logRecord { markers = setOf(LogMarker.AUDIT, LogMarker.SECURITY) }

            format.rendered(record) shouldBe "$HEADER[AUDIT,SECURITY] loaded the servers\n"
        }

        test("escapes line breaks so a message cannot forge a second record") {
            val record = logRecord { message = "first\nINFO forged" }

            format.rendered(record) shouldBe "${HEADER}first\\nINFO forged\n"
        }

        test("renders the cause below the line") {
            val record = logRecord().copied(cause = FAILURE)

            format.rendered(record) shouldContain "java.lang.IllegalStateException: the inventory could not be loaded\n"
        }

        test("renders without escape sequences when the sink supports no color") {
            format.rendered(logRecord { level = LogLevel.Error }) shouldNotContain "\u001B"
        }
    }

    context("logger") {
        test("abbreviates leading packages until the name fits") {
            val abbreviated = plain {
                zone = TimeZone.UTC
                layout { logger(abbreviation = ABBREVIATION) }
            }

            abbreviated.rendered(logRecord()) shouldBe "c.e.i.ServerService\n"
        }

        test("keeps a name that fits") {
            val full = plain { layout { logger(abbreviation = ABBREVIATION) } }

            full.rendered(logRecord { logger = "kide.logging" }) shouldBe "kide.logging\n"
        }
    }

    context("timestamps") {
        test("renders every format") {
            val formats = TimestampFormat.entries.associateWith { timestampFormat ->
                plain {
                    zone = FixedOffsetTimeZone(UtcOffset(hours = 1))
                    layout { timestamp(timestampFormat) }
                }.rendered(logRecord())
            }

            formats shouldBe mapOf(
                TimestampFormat.Time to "09:15:30.250\n",
                TimestampFormat.DateTime to "2026-11-10 09:15:30.250\n",
                TimestampFormat.Iso to "2026-11-10T09:15:30.250+01:00\n",
                TimestampFormat.IsoUtc to "2026-11-10T08:15:30.250Z\n",
                TimestampFormat.EpochMillis to "1794298530250\n",
            )
        }

        test("writes Z for a zone without offset") {
            val utc = plain {
                zone = TimeZone.UTC
                layout { timestamp(TimestampFormat.Iso) }
            }

            utc.rendered(logRecord()) shouldBe "2026-11-10T08:15:30.250Z\n"
        }

        test("writes a negative offset") {
            val western = plain {
                zone = FixedOffsetTimeZone(UtcOffset(hours = -5))
                layout { timestamp(TimestampFormat.Iso) }
            }

            western.rendered(logRecord()) shouldBe "2026-11-10T03:15:30.250-05:00\n"
        }
    }

    context("validation") {
        test("refuses an empty layout") {
            plainReport { layout { } }.isValid shouldBe false
        }

        test("warns when the layout has no message") {
            val report = plainReport { layout { level() } }

            report.isValid shouldBe true
            report.warnings.size shouldBe 1
        }

        test("refuses negative and contradicting widths") {
            val report = plainReport {
                layout {
                    message { minWidth = -1 }
                    sequence { maxWidth = -1 }
                    thread {
                        minWidth = ABBREVIATION
                        maxWidth = 1
                    }
                    logger(abbreviation = -1)
                    cause {
                        maxFrames = -1
                        maxCauses = -1
                    }
                }
            }

            report.errors.size shouldBe INVALID_SEGMENTS
        }

        test("accepts every segment") {
            val report = plainReport {
                layout {
                    text("|")
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
            }

            report.violations shouldBe emptyList()
        }
    }

    context("description") {
        test("names its color mode, zone and segment count") {
            format.toString() shouldContain "segments=13"
        }

        test("keeps the theme and the depth independent") {
            format.theme shouldBe PlainTheme.Default
            format.rendered(logRecord(), ColorDepth.Ansi16) shouldContain "\u001B["
        }
    }
})

private fun plainReport(block: PlainFormatBuilder.() -> Unit): ValidationReport {
    val builder = PlainFormatBuilder()
    builder.block()
    return builder.validate()
}
