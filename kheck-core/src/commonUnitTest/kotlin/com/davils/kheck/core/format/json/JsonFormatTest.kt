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

package com.davils.kheck.core.format.json

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.record.LogMarker
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kheck.core.format.fixture.READER_FRAME
import com.davils.kheck.core.format.fixture.rendered
import com.davils.kheck.core.record.LogCause
import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.style.ColorMode
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val SEQUENCE: Long = 7L
private const val LINE: Int = 17
private const val COUNT: Long = 3L
private const val TIMESTAMP: String = "2026-11-10T08:15:30.250Z"
private const val LOGGER: String = "com.example.inventory.ServerService"
private const val STACK: String = """java.io.IOException: disk full\n\tat com.example.Reader.read(Reader.kt:5)"""
private const val FIELDS: String = """{"rack":"r1","count":3}"""
private const val CONTEXT: String = """{"tenant":"acme"}"""

class JsonFormatTest : FunSpec({
    context("Kide layout") {
        test("writes every part of a full record") {
            json().rendered(full()) shouldBe line(
                """{"timestamp":"$TIMESTAMP","sequence":7,"level":"WARN","logger":"$LOGGER","thread":"main",""",
                """"message":"loaded the servers","markers":["AUDIT"],""",
                """"caller":{"className":"com.example.Service","method":"load","file":"Service.kt","line":17},""",
                """"fields":$FIELDS,"context":$CONTEXT,""",
                """"error":{"type":"java.io.IOException","message":"disk full","stack":"$STACK"}}""",
            )
        }

        test("leaves out what a record does not have") {
            json { isSequenceIncluded = false }.rendered(logRecord()) shouldBe line(
                """{"timestamp":"$TIMESTAMP","level":"INFO","logger":"$LOGGER","thread":"main",""",
                """"message":"loaded the servers"}""",
            )
        }
    }

    context("ECS layout") {
        test("writes a full record with the names of the Elastic Common Schema") {
            json { layout = JsonLayout.Ecs }.rendered(full()) shouldBe line(
                """{"@timestamp":"$TIMESTAMP","log.level":"warn","log.logger":"$LOGGER",""",
                """"process.thread.name":"main",""",
                """"message":"loaded the servers","event.sequence":7,"tags":["AUDIT"],""",
                """"log.origin.function":"com.example.Service.load","log.origin.file.name":"Service.kt",""",
                """"log.origin.file.line":17,"error.type":"java.io.IOException","error.message":"disk full",""",
                """"error.stack_trace":"$STACK",""",
                """"kheck":{"fields":$FIELDS,"context":$CONTEXT},"ecs.version":"8.11.0"}""",
            )
        }

        test("leaves out empty fields inside the kheck block") {
            val format = json { layout = JsonLayout.Ecs }

            val rendered = format.rendered(logRecord { fields { field(name = "rack", value = "r1") } })

            rendered shouldContain """"kheck":{"fields":{"rack":"r1"}}"""
        }

        test("leaves out the kheck block of a record without fields") {
            val minimal = json {
                layout = JsonLayout.Ecs
                isSequenceIncluded = false
            }

            minimal.rendered(logRecord()) shouldBe line(
                """{"@timestamp":"$TIMESTAMP","log.level":"info","log.logger":"$LOGGER",""",
                """"process.thread.name":"main",""",
                """"message":"loaded the servers","ecs.version":"8.11.0"}""",
            )
            minimal.rendered(
                logRecord { context { field(name = "tenant", value = "acme") } },
            ) shouldContain """"kheck":{"context":$CONTEXT}"""
        }
    }

    context("Logstash layout") {
        test("writes a full record with the names of Logstash") {
            json { layout = JsonLayout.Logstash }.rendered(full()) shouldBe line(
                """{"@timestamp":"$TIMESTAMP","@version":"1","message":"loaded the servers","logger_name":"$LOGGER",""",
                """"thread_name":"main","level":"WARN","level_value":30000,"sequence":7,"tags":["AUDIT"],""",
                """"caller_class_name":"com.example.Service","caller_method_name":"load",""",
                """"caller_file_name":"Service.kt",""",
                """"caller_line_number":17,"stack_trace":"$STACK","fields":$FIELDS,"context":$CONTEXT}""",
            )
        }
    }

    context("minimal records") {
        test("leave out caller, cause and entries in the Logstash and OpenTelemetry layouts") {
            val logstash = json {
                layout = JsonLayout.Logstash
                isSequenceIncluded = false
            }
            val opentelemetry = json { layout = JsonLayout.OpenTelemetry }

            logstash.rendered(logRecord()) shouldBe line(
                """{"@timestamp":"$TIMESTAMP","@version":"1","message":"loaded the servers",""",
                """"logger_name":"$LOGGER","thread_name":"main","level":"INFO","level_value":20000}""",
            )
            val rendered = opentelemetry.rendered(logRecord())

            rendered shouldContain """"attributes":{"thread.name":"main","log.record.sequence":1}"""
        }
    }

    context("OpenTelemetry layout") {
        test("writes a full record with the log data model of OpenTelemetry") {
            json { layout = JsonLayout.OpenTelemetry }.rendered(full()) shouldBe line(
                """{"time_unix_nano":"1794298530250000000","severity_number":13,"severity_text":"WARN",""",
                """"body":"loaded the servers","attributes":{"tenant":"acme","rack":"r1","count":3,""",
                """"thread.name":"main",""",
                """"log.record.sequence":7,"log.markers":["AUDIT"],"code.namespace":"com.example.Service",""",
                """"code.function":"load","code.lineno":17,"exception.type":"java.io.IOException",""",
                """"exception.message":"disk full","exception.stacktrace":"$STACK"},""",
                """"instrumentation_scope":{"name":"$LOGGER"}}""",
            )
        }

        test("writes a cause without message as null") {
            val format = json {
                layout = JsonLayout.OpenTelemetry
                isSequenceIncluded = false
            }
            val record = logRecord().copied(cause = LogCause(type = "java.lang.Error", message = null))

            format.rendered(record) shouldContain """"exception.message":null"""
        }
    }

    context("values") {
        test("writes every value with its JSON type") {
            val record = logRecord {
                fields {
                    field("decimal", 1.5)
                    field("infinite", Double.POSITIVE_INFINITY)
                    field("flag", true)
                    timestamp("at", Instant.parse("2026-01-01T00:00:00Z"))
                    duration("took", 2.seconds)
                    secret("key")
                    field("list", LogValue.Items(listOf(LogValue.Integer(1L), LogValue.Null)))
                    group("nested") { field("depth", 2) }
                }
            }

            json().rendered(record) shouldContain
                """"fields":{"decimal":1.5,"infinite":"Infinity","flag":true,"at":"2026-01-01T00:00:00Z",""" +
                """"took":"PT2S","key":"****","list":[1,null],"nested":{"depth":2}}"""
        }
    }

    context("timestamps") {
        test("writes milli, micro and nano seconds") {
            val record = logRecord { timestamp = Instant.parse("2026-11-10T08:15:30.123456789Z") }

            val rendered = TimestampPrecision.entries.map { precision ->
                json { this.precision = precision }
                    .rendered(
                        record,
                    )
            }

            rendered.map { text -> text.substringAfter("08:15:").substringBefore('"') } shouldBe
                listOf("30.123Z", "30.123456Z", "30.123456789Z")
        }
    }

    context("configuration") {
        test("refuses negative limits for stack traces") {
            val builder = JsonFormatBuilder()
            builder.maxFrames = -1
            builder.maxCauses = -1

            builder.validate().errors.map { violation -> violation.code } shouldBe
                listOf(LoggingViolationCodes.INVALID_FRAMES, LoggingViolationCodes.INVALID_FRAMES)
        }

        test("never colors and describes itself") {
            val format = json {
                exclude("kotlinx.coroutines")
                maxFrames = 1
                maxCauses = 1
            }

            format.colorMode shouldBe ColorMode.Never
            format.excludedPackages shouldBe listOf("kotlinx.coroutines")
            format.toString() shouldBe "JsonFormat(layout=Standard, precision=Milliseconds)"
        }
    }
})

private fun full(): LogRecord = logRecord {
    sequence = SEQUENCE
    level = LogLevel.Warn
    caller = LogFrame(
        className = "com.example.Service",
        methodName = "load",
        fileName = "Service.kt",
        lineNumber = LINE,
    )
    markers = setOf(LogMarker.AUDIT)
    fields {
        field(name = "rack", value = "r1")
        field("count", COUNT)
    }
    context { field(name = "tenant", value = "acme") }
}.copied(cause = LogCause(type = "java.io.IOException", message = "disk full", frames = listOf(READER_FRAME)))

private fun line(vararg parts: String): String = parts.joinToString(separator = "", postfix = "\n")
