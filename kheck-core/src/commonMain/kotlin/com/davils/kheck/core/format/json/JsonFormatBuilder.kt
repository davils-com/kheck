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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder

/**
 * Configures a [JsonFormat] inside [json].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class JsonFormatBuilder internal constructor() : ValidatingBuilder<JsonFormat>() {
    /**
     * Names the format in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "json format"

    /**
     * The shape of the objects; [JsonLayout.Standard] by default.
     *
     * @since 1.0.0
     */
    public var layout: JsonLayout = JsonLayout.Standard

    /**
     * The digits of the timestamps; [TimestampPrecision.Milliseconds] by default.
     *
     * @since 1.0.0
     */
    public var precision: TimestampPrecision = TimestampPrecision.Milliseconds

    /**
     * Whether the sequence number is written; `true` by default.
     *
     * @since 1.0.0
     */
    public var isSequenceIncluded: Boolean = true

    /**
     * The most frames of the stack trace written per throwable; [DEFAULT_MAX_FRAMES] by default.
     *
     * @since 1.0.0
     */
    public var maxFrames: Int = DEFAULT_MAX_FRAMES

    /**
     * The longest chain of causes written; [DEFAULT_MAX_CAUSES] by default.
     *
     * @since 1.0.0
     */
    public var maxCauses: Int = DEFAULT_MAX_CAUSES

    internal val excluded = mutableListOf<String>()

    /**
     * Leaves out the frames of classes whose name starts with one of [packages].
     *
     * @param packages the prefixes of class names.
     * @since 1.0.0
     */
    public fun exclude(vararg packages: String) {
        checkOpen()
        excluded += packages
    }

    /**
     * Creates the format.
     *
     * @return the format, validated afterwards.
     * @since 1.0.0
     */
    override fun assemble(): JsonFormat = JsonFormat(this)

    /**
     * The defaults of the format.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The default of [maxFrames].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_MAX_FRAMES: Int = 64

        /**
         * The default of [maxCauses].
         *
         * @since 1.0.0
         */
        public const val DEFAULT_MAX_CAUSES: Int = 8
    }
}
