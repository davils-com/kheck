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

import com.davils.kheck.core.format.plain.builder.PlainLayoutBuilder
import com.davils.kheck.core.style.ColorMode
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlinx.datetime.TimeZone

/**
 * Configures a [PlainFormat] inside [plain].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class PlainFormatBuilder internal constructor() : ValidatingBuilder<PlainFormat>() {
    /**
     * Names the format in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "plain format"

    /**
     * Whether the layout writes colors; [ColorMode.Auto][com.davils.kheck.core.style.ColorMode.Auto] by default.
     *
     * @since 1.0.0
     */
    public var colorMode: ColorMode = ColorMode.Auto

    /**
     * The styles the segments start from; [PlainTheme.Default] by default.
     *
     * @since 1.0.0
     */
    public var theme: PlainTheme = PlainTheme.Default

    /**
     * The time zone of local timestamps; the zone of the system by default.
     *
     * @since 1.0.0
     */
    public var zone: TimeZone = TimeZone.currentSystemDefault()

    private var layoutBlock: PlainLayoutBuilder.() -> Unit = DEFAULT_LAYOUT

    /**
     * Replaces the segments of a line; [DEFAULT_LAYOUT] unless set.
     *
     * @param block declares the segments in order.
     * @since 1.0.0
     */
    public fun layout(block: PlainLayoutBuilder.() -> Unit) {
        checkOpen()
        layoutBlock = block
    }

    internal fun segments(): List<PlainSegment> {
        val builder = PlainLayoutBuilder(theme)
        builder.layoutBlock()
        return builder.segments()
    }

    /**
     * Creates the format.
     *
     * @return the format, validated afterwards.
     * @since 1.0.0
     */
    override fun assemble(): PlainFormat = PlainFormat(this)

    /**
     * The default layout.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val LOGGER_ABBREVIATION: Int = 40

        /**
         * The default segments: `2026-11-24 14:03:27.512 INFO  [main] c.e.b.Invoices - [AUDIT] message key=value`,
         * followed by the stack trace.
         *
         * @since 1.0.0
         */
        public val DEFAULT_LAYOUT: PlainLayoutBuilder.() -> Unit = {
            timestamp()
            text(" ")
            level()
            text(" [")
            thread()
            text("] ")
            logger(abbreviation = LOGGER_ABBREVIATION)
            text(" - ")
            markers()
            message()
            fields()
            context()
            cause()
        }
    }
}
