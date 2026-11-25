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
import com.davils.kheck.core.format.fixture.ESCAPE
import com.davils.kheck.core.format.fixture.RESET
import com.davils.kheck.core.format.fixture.rendered
import com.davils.kheck.core.style.Color
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.testing.logRecord
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val PALETTE_INDEX: Int = 110
private const val WIDTH: Int = 8
private const val NARROW: Int = 6

class PlainStyleTest : FunSpec({
    context("segments") {
        test("colors every segment on its own") {
            val format = plain {
                layout {
                    level { style { foreground = Color.Red } }
                    text(" ")
                    message {
                        style {
                            isBold = true
                            isItalic = true
                        }
                    }
                }
            }

            format.rendered(logRecord(), ColorDepth.Ansi16) shouldBe
                "${ESCAPE}31mINFO$RESET  ${ESCAPE}1;3mloaded the servers$RESET\n"
        }

        test("chooses the style of the level of the record") {
            val format = plain {
                layout {
                    message {
                        style {
                            byLevel {
                                trace { isDim = true }
                                debug { isUnderline = true }
                                info { isStrikethrough = true }
                                warn { foreground = Color.Yellow }
                                error { background = Color.BrightRed }
                            }
                        }
                    }
                }
            }
            val levels = LogLevel.entries - LogLevel.Off
            val rendered = levels.map { candidate ->
                format.rendered(
                    logRecord { level = candidate },
                    ColorDepth.Ansi16,
                )
            }

            rendered.map { line -> line.substringBefore('m').removePrefix(ESCAPE) } shouldBe
                listOf("2", "4", "9", "33", "101")
        }

        test("styles keys, values and punctuation of entries apart") {
            val format = plain {
                layout {
                    fields {
                        prefix = ""
                        key { foreground = Color.Blue }
                        value { foreground = Color.Green }
                        punctuation { foreground = Color.BrightBlack }
                    }
                }
            }
            val record = logRecord { fields { field(name = "rack", value = "r1") } }

            format.rendered(record, ColorDepth.Ansi16) shouldBe
                "${ESCAPE}34mrack$RESET${ESCAPE}90m=$RESET${ESCAPE}32mr1$RESET\n"
        }

        test("styles a text segment") {
            val format = plain { layout { text("|") { background = Color.Indexed(PALETTE_INDEX) } } }

            format.rendered(logRecord(), ColorDepth.Ansi256) shouldBe "${ESCAPE}48;5;${PALETTE_INDEX}m|$RESET\n"
        }
    }

    context("color depth") {
        val format = plain { layout { message { style { foreground = Color.hex("#FF8000") } } } }

        test("keeps a true color on a true color terminal") {
            format.rendered(logRecord(), ColorDepth.TrueColor) shouldBe
                "${ESCAPE}38;2;255;128;0mloaded the servers$RESET\n"
        }

        test("reduces a true color to the palette of 256 colors") {
            format.rendered(logRecord(), ColorDepth.Ansi256) shouldBe
                "${ESCAPE}38;5;208mloaded the servers$RESET\n"
        }

        test("reduces a true color to the nearest of 16 colors") {
            format.rendered(logRecord(), ColorDepth.Ansi16) shouldBe "${ESCAPE}33mloaded the servers$RESET\n"
        }

        test("reduces a palette color to the nearest of 16 colors") {
            val indexed = plain { layout { message { style { foreground = Color.Indexed(PALETTE_INDEX) } } } }

            indexed.rendered(logRecord(), ColorDepth.Ansi16) shouldBe "${ESCAPE}90mloaded the servers$RESET\n"
        }

        test("reduces a gray to the gray ramp of the palette") {
            val gray = plain { layout { message { style { foreground = Color.hex("#808080") } } } }

            gray.rendered(logRecord(), ColorDepth.Ansi256) shouldBe "${ESCAPE}38;5;244mloaded the servers$RESET\n"
        }

        test("writes no escape sequence without color") {
            format.rendered(logRecord(), ColorDepth.None) shouldBe "loaded the servers\n"
        }
    }

    context("frames") {
        test("pads behind a segment aligned to the start") {
            val format = plain {
                layout {
                    thread { minWidth = WIDTH }
                    text("|")
                }
            }

            format.rendered(logRecord()) shouldBe "main    |\n"
        }

        test("pads in front of a segment aligned to the end") {
            val format = plain {
                layout {
                    thread {
                        minWidth = WIDTH
                        alignment = Alignment.End
                    }
                }
            }

            format.rendered(logRecord()) shouldBe "    main\n"
        }

        test("cuts the end of a segment aligned to the start") {
            val format = plain { layout { message { maxWidth = NARROW } } }

            format.rendered(logRecord()) shouldBe "loaded\n"
        }

        test("cuts the start of a segment aligned to the end") {
            val format = plain {
                layout {
                    message {
                        maxWidth = NARROW
                        alignment = Alignment.End
                    }
                }
            }

            format.rendered(logRecord()) shouldBe "ervers\n"
        }
    }

    context("themes") {
        test("colors the default layout of every theme") {
            val themes = listOf(
                PlainTheme.Default,
                PlainTheme.Muted,
                PlainTheme.HighContrast,
                PlainTheme.Monochrome,
            )

            themes.forEach { theme ->
                val format = plain { this.theme = theme }
                val rendered = format.rendered(logRecord { level = LogLevel.Warn }, ColorDepth.Ansi16)
                rendered.contains(ESCAPE) shouldBe true
            }
        }
    }
})
