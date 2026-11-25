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

import com.davils.kheck.core.style.Color
import com.davils.kheck.core.style.SegmentStyle
import com.davils.kheck.core.style.segmentStyle
import com.davils.kheck.core.style.textStyle

/**
 * The styles a plain layout starts from; a segment of the layout may override its own style.
 *
 * The styles are drawn only by a sink whose terminal shows colors.
 *
 * @property timestamp the style of the time.
 * @property level the style of the level.
 * @property thread the style of the thread.
 * @property logger the style of the logger.
 * @property caller the style of the caller.
 * @property markers the style of the markers.
 * @property message the style of the message.
 * @property entries the styles of fields and context.
 * @property cause the styles of the stack trace.
 * @since 1.0.0
 */
public class PlainTheme(
    public val timestamp: SegmentStyle = SegmentStyle.NONE,
    public val level: SegmentStyle = SegmentStyle.NONE,
    public val thread: SegmentStyle = SegmentStyle.NONE,
    public val logger: SegmentStyle = SegmentStyle.NONE,
    public val caller: SegmentStyle = SegmentStyle.NONE,
    public val markers: SegmentStyle = SegmentStyle.NONE,
    public val message: SegmentStyle = SegmentStyle.NONE,
    public val entries: EntryStyles = EntryStyles(),
    public val cause: CauseStyles = CauseStyles(),
) {
    /**
     * Shows the style of the level and of the message.
     *
     * @return the theme as text.
     * @since 1.0.0
     */
    override fun toString(): String = "PlainTheme(level=$level, message=$message)"

    /**
     * The themes kheck offers.
     *
     * @since 1.0.0
     */
    public companion object {
        private val defaultLevel: SegmentStyle = segmentStyle {
            isBold = true
            byLevel {
                trace { foreground = Color.BrightBlack }
                debug { foreground = Color.Cyan }
                info { foreground = Color.Green }
                warn { foreground = Color.Yellow }
                error { foreground = Color.Red }
            }
        }

        private val defaultMarkers: SegmentStyle = segmentStyle {
            foreground = Color.Magenta
            isBold = true
        }

        private val defaultCause: CauseStyles = CauseStyles(
            type = textStyle {
                foreground = Color.Red
                isBold = true
            },
            message = textStyle { foreground = Color.Red },
            label = textStyle {
                foreground = Color.Yellow
                isBold = true
            },
            frame = textStyle { foreground = Color.BrightBlack },
            location = textStyle { foreground = Color.Cyan },
        )

        /**
         * Bold for the levels from warn, the markers and the labels of a stack trace, and no colors.
         *
         * @since 1.0.0
         */
        public val Monochrome: PlainTheme = PlainTheme(
            level = segmentStyle {
                byLevel {
                    warn { isBold = true }
                    error { isBold = true }
                }
            },
            markers = segmentStyle { isBold = true },
            cause = CauseStyles(type = textStyle { isBold = true }, label = textStyle { isBold = true }),
        )

        /**
         * Colored levels, a gray time and thread, a cyan logger and a red stack trace.
         *
         * @since 1.0.0
         */
        public val Default: PlainTheme = PlainTheme(
            timestamp = segmentStyle { foreground = Color.BrightBlack },
            level = defaultLevel,
            thread = segmentStyle { foreground = Color.BrightBlack },
            logger = segmentStyle { foreground = Color.Cyan },
            caller = segmentStyle {
                foreground = Color.BrightBlack
                isItalic = true
            },
            markers = defaultMarkers,
            message = segmentStyle {
                byLevel {
                    warn { foreground = Color.Yellow }
                    error { foreground = Color.BrightRed }
                }
            },
            entries = EntryStyles(
                key = segmentStyle { foreground = Color.Blue },
                punctuation = segmentStyle { foreground = Color.BrightBlack },
            ),
            cause = defaultCause,
        )

        /**
         * The colored levels of [Default] with everything around them dimmed.
         *
         * @since 1.0.0
         */
        public val Muted: PlainTheme = PlainTheme(
            timestamp = segmentStyle { isDim = true },
            level = defaultLevel,
            thread = segmentStyle { isDim = true },
            logger = segmentStyle { isDim = true },
            caller = segmentStyle { isDim = true },
            markers = defaultMarkers,
            entries = EntryStyles(key = segmentStyle { isDim = true }, punctuation = segmentStyle { isDim = true }),
            cause = defaultCause,
        )

        /**
         * Bright colors and backgrounds for warnings and errors, for terminals with low contrast.
         *
         * @since 1.0.0
         */
        public val HighContrast: PlainTheme = PlainTheme(
            timestamp = segmentStyle { foreground = Color.BrightWhite },
            level = segmentStyle {
                isBold = true
                byLevel {
                    trace { foreground = Color.BrightWhite }
                    debug { foreground = Color.BrightCyan }
                    info { foreground = Color.BrightGreen }
                    warn {
                        foreground = Color.Black
                        background = Color.BrightYellow
                    }
                    error {
                        foreground = Color.BrightWhite
                        background = Color.Red
                    }
                }
            },
            thread = segmentStyle { foreground = Color.BrightWhite },
            logger = segmentStyle { foreground = Color.BrightCyan },
            caller = Default.caller,
            markers = defaultMarkers,
            message = Default.message,
            entries = EntryStyles(
                key = segmentStyle { foreground = Color.BrightBlue },
                value = segmentStyle { foreground = Color.BrightWhite },
                punctuation = segmentStyle { foreground = Color.BrightWhite },
            ),
            cause = defaultCause,
        )
    }
}
