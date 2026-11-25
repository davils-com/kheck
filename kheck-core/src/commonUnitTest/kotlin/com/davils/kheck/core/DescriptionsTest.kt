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

import com.davils.kheck.core.fixture.testLogging
import com.davils.kheck.core.format.plain.CauseLayout
import com.davils.kheck.core.format.plain.CauseStyles
import com.davils.kheck.core.format.plain.EntriesLayout
import com.davils.kheck.core.format.plain.EntryStyles
import com.davils.kheck.core.format.plain.PlainTheme
import com.davils.kheck.core.format.plain.SegmentAffix
import com.davils.kheck.core.format.plain.SegmentFrame
import com.davils.kheck.core.format.plain.plain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest

private const val WIDTH: Int = 5

class DescriptionsTest : FunSpec({
    context("the logging") {
        test("shows its sinks and whether it is closed") {
            runTest {
                val logging = testLogging()

                logging.toString() shouldBe "Logging(sinks=[], isClosed=false)"
                Reconfiguration.Applied.toString() shouldBe "Applied"
                Reconfiguration.Closed.toString() shouldBe "Closed"
                val rejected = logging.reconfigure { intake { capacity = WIDTH } }
                rejected.toString() shouldBe "Rejected(errors=1)"
            }
        }
    }

    context("plain layouts") {
        test("show their parts") {
            val frame = SegmentFrame(minWidth = 1, maxWidth = WIDTH)
            val affix = SegmentAffix(prefix = "[", suffix = "]")

            frame shouldBe SegmentFrame(minWidth = 1, maxWidth = WIDTH)
            frame.hashCode() shouldBe SegmentFrame(minWidth = 1, maxWidth = WIDTH).hashCode()
            frame shouldNotBe SegmentFrame.NONE
            frame shouldNotBe SegmentFrame(minWidth = 1, maxWidth = 1)
            frame.equals(affix) shouldBe false
            frame.toString() shouldBe "SegmentFrame(minWidth=1, maxWidth=5, alignment=Start)"
            affix shouldBe SegmentAffix(prefix = "[", suffix = "]")
            affix.hashCode() shouldBe SegmentAffix(prefix = "[", suffix = "]").hashCode()
            affix shouldNotBe SegmentAffix.NONE
            affix shouldNotBe SegmentAffix(prefix = "[")
            affix.toString() shouldBe """SegmentAffix(prefix="[", suffix="]")"""
            EntriesLayout(affix, ",", EntryStyles()).toString() shouldBe
                """EntriesLayout(affix=SegmentAffix(prefix="[", suffix="]"), separator=",")"""
            CauseLayout(
                maxFrames = 1,
                maxCauses = 1,
                isSuppressedIncluded = true,
                excludedPackages = listOf("a."),
                indent = "",
                styles = CauseStyles(),
            ).toString() shouldContain "excludedPackages=[a.]"
            CauseStyles().toString() shouldContain "CauseStyles(type="
            EntryStyles().toString() shouldContain "EntryStyles(key="
            PlainTheme.Muted.toString() shouldContain "PlainTheme(level="
            plain().theme shouldBe PlainTheme.Default
        }
    }
})
