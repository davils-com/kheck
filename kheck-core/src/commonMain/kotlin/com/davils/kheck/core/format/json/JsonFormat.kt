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

import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kheck.core.format.LogEncoder
import com.davils.kheck.core.format.LogFormat
import com.davils.kheck.core.format.json.encoding.JsonEncoder
import com.davils.kheck.core.style.ColorDepth
import com.davils.kheck.core.style.ColorMode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The JSON format: one JSON object per line and record, in the shape of a [JsonLayout].
 *
 * The objects follow RFC 8259. Every string is escaped as JSON requires, and every code point that could forge a line
 * or control a terminal, such as a bidirectional override or a line separator, is written as a `\u` escape as well,
 * so the text stays exact and safe to show. A text longer than
 * [Limits.MAX_TEXT_LENGTH][com.davils.kheck.core.Limits.MAX_TEXT_LENGTH] is shortened and marked. A number that is
 * not finite is written as a string. A [secret][com.davils.kheck.api.record.LogValue.Secret] is written as the mask.
 * Created by [json]; it never writes colors.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class JsonFormat internal constructor(builder: JsonFormatBuilder) : LogFormat, Validatable {
    /**
     * Always [ColorMode.Never].
     *
     * @since 1.0.0
     */
    override val colorMode: ColorMode = ColorMode.Never

    /**
     * The shape of the objects.
     *
     * @since 1.0.0
     */
    public val layout: JsonLayout = builder.layout

    /**
     * The digits of the timestamps.
     *
     * @since 1.0.0
     */
    public val precision: TimestampPrecision = builder.precision

    /**
     * Whether the sequence number is written.
     *
     * @since 1.0.0
     */
    public val isSequenceIncluded: Boolean = builder.isSequenceIncluded

    /**
     * The most frames written per throwable.
     *
     * @since 1.0.0
     */
    public val maxFrames: Int = builder.maxFrames

    /**
     * The longest chain of causes written.
     *
     * @since 1.0.0
     */
    public val maxCauses: Int = builder.maxCauses

    /**
     * The prefixes of class names whose frames are left out; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val excludedPackages: List<String> = builder.excluded.immutableList()

    /**
     * Creates the encoder of one sink.
     *
     * @param depth ignored, because JSON has no colors.
     * @return a new encoder.
     * @since 1.0.0
     */
    override fun encoder(depth: ColorDepth): LogEncoder = JsonEncoder(this)

    /**
     * Checks that the limits of the stack trace are not negative.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        if (maxFrames < 0) fail(::maxFrames, "must not be negative", LoggingViolationCodes.INVALID_FRAMES)
        if (maxCauses >= 0) return
        fail(::maxCauses, "must not be negative", LoggingViolationCodes.INVALID_FRAMES)
    }

    /**
     * Shows the layout and the precision.
     *
     * @return the format as text.
     * @since 1.0.0
     */
    override fun toString(): String = "JsonFormat(layout=$layout, precision=$precision)"
}
