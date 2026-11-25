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

package com.davils.kheck.api

/**
 * The upper bounds kheck enforces on what a caller logs.
 *
 * A record that exceeds a bound is never refused, because logging must not fail the code that logs, and it is never
 * shortened silently either: what does not fit is left out and counted, and every layout of kheck writes that count
 * with the record, as [com.davils.kheck.api.record.LogEvent.omitted] describes. Text is bounded where it is written,
 * by the limits of the layout.
 *
 * The limits are part of the public contract: raising one is a compatible change, lowering one a breaking change.
 *
 * @since 1.0.0
 */
public object Limits {
    /**
     * The most fields one level of a record holds; a field beyond it is left out and counted.
     *
     * @since 1.0.0
     */
    public const val MAX_FIELDS: Int = 256

    /**
     * The deepest nesting of groups and lists in the fields of a record; a deeper value is left out and counted.
     *
     * @since 1.0.0
     */
    public const val MAX_DEPTH: Int = 16

    /**
     * The most values one list in a field holds; a value beyond it is left out and counted.
     *
     * @since 1.0.0
     */
    public const val MAX_ITEMS: Int = 1_024

    /**
     * The most markers one record carries; a marker beyond it is left out and counted.
     *
     * @since 1.0.0
     */
    public const val MAX_MARKERS: Int = 32

    /**
     * The longest name of a logger, of a field or of a marker, in UTF-16 code units.
     *
     * A longer name of a logger or a marker is refused with an [IllegalArgumentException] where it is created; a field
     * with a longer name is left out and counted.
     *
     * @since 1.0.0
     */
    public const val MAX_NAME_LENGTH: Int = 512
}
