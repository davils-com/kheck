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

/**
 * How many digits of the second a JSON timestamp carries.
 *
 * @property digits the digits after the decimal point.
 * @since 1.0.0
 */
public enum class TimestampPrecision(public val digits: Int) {
    /**
     * Three digits, such as `2026-11-24T13:03:27.512Z`.
     *
     * @since 1.0.0
     */
    Milliseconds(MILLISECOND_DIGITS),

    /**
     * Six digits.
     *
     * @since 1.0.0
     */
    Microseconds(MICROSECOND_DIGITS),

    /**
     * Nine digits.
     *
     * @since 1.0.0
     */
    Nanoseconds(NANOSECOND_DIGITS),
}

private const val MILLISECOND_DIGITS: Int = 3
private const val MICROSECOND_DIGITS: Int = 6
private const val NANOSECOND_DIGITS: Int = 9
