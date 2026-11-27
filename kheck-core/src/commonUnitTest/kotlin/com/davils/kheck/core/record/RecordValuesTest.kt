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

package com.davils.kheck.core.record

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.testing.logRecord
import com.davils.kore.testing.HostileFragments
import com.davils.kore.testing.isSafeToShow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.time.Instant

private const val LINE: Int = 12
private const val OMITTED: Int = 2
private const val CLASS: String = "a.B"
private const val METHOD: String = "run"
private const val FILE: String = "B.kt"
private const val MAIN: String = "main"
private val FRAME: LogFrame = LogFrame(className = CLASS, methodName = METHOD, fileName = FILE, lineNumber = LINE)
private val LOGGER: LoggerName = LoggerName("a")

class RecordValuesTest : FunSpec({
    context("frames") {
        test("compare every part and show themselves as a stack trace does") {
            val same = LogFrame(className = CLASS, methodName = METHOD, fileName = FILE, lineNumber = LINE)
            val bare = LogFrame(className = CLASS, methodName = METHOD)

            FRAME shouldBe same
            FRAME.hashCode() shouldBe same.hashCode()
            FRAME shouldNotBe LogFrame(className = "a.C", methodName = METHOD, fileName = FILE, lineNumber = LINE)
            FRAME shouldNotBe LogFrame(className = CLASS, methodName = "go", fileName = FILE, lineNumber = LINE)
            FRAME shouldNotBe LogFrame(className = CLASS, methodName = METHOD, fileName = null, lineNumber = LINE)
            FRAME shouldNotBe LogFrame(className = CLASS, methodName = METHOD, fileName = FILE)
            FRAME.equals(CLASS) shouldBe false
            FRAME.toString() shouldBe "a.B.run(B.kt:12)"
            bare.toString() shouldBe "a.B.run(unknown:-1)"
            bare.hashCode() shouldBe LogFrame(className = CLASS, methodName = METHOD).hashCode()
        }
    }

    context("sources") {
        test("compare logger, thread and caller and escape the thread") {
            val source = LogSource(logger = LOGGER, thread = MAIN, caller = FRAME)
            val same = LogSource(logger = LOGGER, thread = MAIN, caller = FRAME)
            val bare = LogSource(logger = LOGGER, thread = MAIN)
            val hostile = LogSource(logger = LOGGER, thread = "x${HostileFragments.CLEAR_SCREEN}")

            source shouldBe same
            source.hashCode() shouldBe same.hashCode()
            source shouldNotBe LogSource(logger = LoggerName("b"), thread = MAIN, caller = FRAME)
            source shouldNotBe LogSource(logger = LOGGER, thread = "worker", caller = FRAME)
            source shouldNotBe bare
            source.equals(MAIN) shouldBe false
            bare.hashCode() shouldBe LogSource(logger = LOGGER, thread = MAIN).hashCode()
            bare.toString() shouldContain "caller=none"
            isSafeToShow(hostile.toString()) shouldBe true
        }
    }

    context("records and causes") {
        test("show no foreign text but the escaped message") {
            val cause = LogCause(
                type = "a.Failure",
                message = "secret",
                frames = listOf(FRAME),
                omittedFrames = OMITTED,
            )
            val record = LogRecord(
                sequence = 1L,
                timestamp = Instant.DISTANT_PAST,
                level = LogLevel.Info,
                source = LogSource(logger = LOGGER, thread = MAIN),
                message = "hello\n",
                cause = cause,
            )

            cause.toString() shouldBe "LogCause(type=a.Failure, frames=1, omittedFrames=2)"
            cause.toString() shouldNotContain "secret"
            record.toString() shouldContain """message="hello\n""""
        }
    }

    context("equality") {
        test("compares records and causes by value") {
            val failure = IllegalStateException("broken")
            val first = logRecord {
                message = "hello"
                cause = failure
            }
            val second = logRecord {
                message = "hello"
                cause = failure
            }

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first.cause shouldBe second.cause
            first shouldNotBe logRecord { message = "other" }
        }
    }

    context("foreign values") {
        test("are never equal to a value of another type") {
            val record = logRecord { cause = IllegalStateException("broken") }

            record.equals("record") shouldBe false
            record.cause?.equals("cause") shouldBe false
        }
    }
})
