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

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.format.fixture.rendered
import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val SEQUENCE: Long = 42L
private const val LINE: Int = 17
private const val WIDE: Int = 80
private const val SHORT: Int = 5
private const val NAMES: Int = 4_100

class PlainSegmentsTest : FunSpec({
    context("optional segments") {
        test("renders the caller with its affixes") {
            val format = plain {
                layout {
                    caller {
                        prefix = "<"
                        suffix = ">"
                    }
                }
            }
            val record = logRecord {
                caller = LogFrame(
                    className = "com.example.Service",
                    methodName = "load",
                    fileName = "Service.kt",
                    lineNumber = LINE,
                )
            }

            format.rendered(record) shouldBe "<com.example.Service.load:17>\n"
        }

        test("leaves out the caller when the record has none") {
            plain { layout { caller() } }.rendered(logRecord()) shouldBe "\n"
        }

        test("renders the sequence") {
            plain { layout { sequence() } }.rendered(logRecord { sequence = SEQUENCE }) shouldBe "42\n"
        }

        test("renders the context with a custom separator") {
            val format = plain {
                layout {
                    context {
                        prefix = "{"
                        suffix = "}"
                        separator = ", "
                    }
                }
            }
            val record = logRecord {
                context {
                    field(name = "tenant", value = "acme")
                    field(name = "user", value = "alice")
                }
            }

            format.rendered(record) shouldBe "{tenant=acme, user=alice}\n"
        }

        test("leaves out empty entries") {
            plain { layout { fields() } }.rendered(logRecord()) shouldBe "\n"
        }
    }

    context("frames and names") {
        test("writes no escape sequence for an empty message") {
            val format = plain { layout { message { style { isBold = true } } } }

            format.rendered(logRecord { message = "" }, ColorDepth.Ansi16) shouldBe "\n"
        }

        test("keeps text that fits into the maximum width") {
            plain { layout { message { maxWidth = WIDE } } }.rendered(logRecord()) shouldBe "loaded the servers\n"
        }

        test("abbreviates names with short segments and remembers every name") {
            val format = plain { layout { logger(abbreviation = SHORT) } }
            val names = (0..NAMES).map { index -> "a.b.Logger$index" }

            names.forEach { name -> format.rendered(logRecord { logger = name }) }

            format.rendered(logRecord { logger = names.first() }) shouldBe "a.b.Logger0\n"
            format.rendered(logRecord { logger = "alpha.beta.Gamma" }) shouldBe "a.b.Gamma\n"
        }
    }

    context("values") {
        test("renders every kind of value") {
            val record = logRecord {
                fields {
                    field(name = "text", value = "plain")
                    field(name = "empty", value = "")
                    field(name = "quoted", value = """say "hi"""")
                    field(name = "missing", value = null as String?)
                    field("decimal", 1.5)
                    field("flag", true)
                    field("list", LogValue.Items(listOf(LogValue.Text("a b"), LogValue.Integer(1L))))
                    group("nested") {
                        field("depth", 2)
                        field("other", 3)
                    }
                    field(name = "equation", value = "a=b")
                    timestamp("at", Instant.parse("2026-01-01T00:00:00Z"))
                    duration("took", 2.seconds)
                }
            }
            val format = plain { layout { fields { prefix = "" } } }

            format.rendered(record) shouldBe
                """text=plain empty="" quoted="say \"hi\"" missing=null decimal=1.5 flag=true """ +
                """list=["a b", 1] nested={depth=2, other=3} equation="a=b" at=2026-01-01T00:00:00Z took=2s""" +
                "\n"
        }
    }
})
