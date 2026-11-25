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

import com.davils.kheck.core.format.fixture.ESCAPE
import com.davils.kheck.core.format.fixture.FAILURE
import com.davils.kheck.core.format.fixture.READER_FRAME
import com.davils.kheck.core.format.fixture.ROOT_FRAME
import com.davils.kheck.core.format.fixture.SERVICE_FRAME
import com.davils.kheck.core.format.fixture.rendered
import com.davils.kheck.core.record.LogCause
import com.davils.kheck.core.record.LogFrame
import com.davils.kheck.core.style.Color
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

private const val OMITTED: Int = 7
private const val SERVICE_LINE: String = "    at com.example.Service.run(Service.kt:10)"
private const val ILLEGAL_STATE: String = "java.lang.IllegalStateException"

class PlainCauseTest : FunSpec({
    val format = plain { layout { cause() } }

    context("stack traces") {
        test("prints the frames of the cause and folds the frames it shares with its parent") {
            format.rendered(logRecord().copied(cause = FAILURE)) shouldBe lines(
                "",
                "java.lang.IllegalStateException: the inventory could not be loaded",
                SERVICE_LINE,
                "    at com.example.Main.main(Main.kt:3)",
                "Caused by: java.io.IOException: disk full",
                "    at com.example.Reader.read(Reader.kt:5)",
                "    ... 1 more",
            )
        }

        test("indents suppressed failures below their parent") {
            val cause = LogCause(
                type = ILLEGAL_STATE,
                message = null,
                frames = listOf(SERVICE_FRAME),
                suppressed = listOf(
                    LogCause(type = "java.io.IOException", message = "close", frames = listOf(READER_FRAME)),
                ),
            )

            format.rendered(logRecord().copied(cause = cause)) shouldBe lines(
                "",
                ILLEGAL_STATE,
                SERVICE_LINE,
                "    Suppressed: java.io.IOException: close",
                "        at com.example.Reader.read(Reader.kt:5)",
            )
        }

        test("leaves suppressed failures out on request") {
            val cause = LogCause(
                type = ILLEGAL_STATE,
                message = "boom",
                suppressed = listOf(LogCause(type = "java.io.IOException", message = "close")),
            )
            val quiet = plain { layout { cause { isSuppressedIncluded = false } } }

            val rendered = quiet.rendered(logRecord().copied(cause = cause))

            rendered shouldBe lines("", "java.lang.IllegalStateException: boom")
        }

        test("counts frames it leaves out because of the limit, the exclusions and the capture") {
            val cause = LogCause(
                type = ILLEGAL_STATE,
                message = "boom",
                frames = listOf(SERVICE_FRAME, frame("kotlinx.coroutines.Dispatched"), READER_FRAME, ROOT_FRAME),
                omittedFrames = OMITTED,
            )
            val short = plain {
                layout {
                    cause {
                        maxFrames = 2
                        exclude("kotlinx.coroutines")
                    }
                }
            }

            short.rendered(logRecord().copied(cause = cause)) shouldBe lines(
                "",
                "java.lang.IllegalStateException: boom",
                SERVICE_LINE,
                "    at com.example.Reader.read(Reader.kt:5)",
                "    ... 9 frames omitted",
            )
        }

        test("stops after the allowed number of causes") {
            val single = plain { layout { cause { maxCauses = 1 } } }

            single.rendered(logRecord().copied(cause = FAILURE)) shouldBe lines(
                "",
                "java.lang.IllegalStateException: the inventory could not be loaded",
                SERVICE_LINE,
                "    at com.example.Main.main(Main.kt:3)",
                "    ... further causes omitted",
            )
        }

        test("names frames without a source file or line") {
            val cause = LogCause(
                type = "java.lang.Error",
                message = null,
                frames = listOf(
                    LogFrame(className = "a.B", methodName = "c"),
                    LogFrame(className = "a.B", methodName = "d", fileName = "B.kt"),
                ),
            )

            format.rendered(logRecord().copied(cause = cause)) shouldBe lines(
                "",
                "java.lang.Error",
                "    at a.B.c(Unknown Source)",
                "    at a.B.d(B.kt)",
            )
        }

        test("colors every part of a frame") {
            val colored = plain {
                layout {
                    cause {
                        type { foreground = Color.Red }
                        message { foreground = Color.Yellow }
                        label { foreground = Color.Magenta }
                        frame { foreground = Color.Blue }
                        location { foreground = Color.Cyan }
                    }
                }
            }

            colored.rendered(logRecord().copied(cause = FAILURE), ColorDepth.Ansi16) shouldStartWith
                "\n${ESCAPE}31mjava.lang.IllegalStateException"
        }

        test("writes nothing below the line for a record without cause") {
            format.rendered(logRecord()) shouldBe "\n"
        }
    }
})

private fun frame(type: String): LogFrame = LogFrame(
    className = type,
    methodName = "resume",
    fileName = "D.kt",
    lineNumber = 1,
)

private fun lines(vararg lines: String): String = lines.joinToString(separator = "\n", postfix = "\n")
