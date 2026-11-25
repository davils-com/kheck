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

package com.davils.kheck.core.sample

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * What a [RateLimit] counts separately.
 *
 * @since 1.0.0
 */
public sealed interface RateLimitKey {
    /**
     * One limit for all records.
     *
     * @since 1.0.0
     */
    public object Global : RateLimitKey {
        /**
         * Shows the name of the key.
         *
         * @return `Global`.
         * @since 1.0.0
         */
        override fun toString(): String = "Global"
    }

    /**
     * One limit per logger.
     *
     * @since 1.0.0
     */
    public object PerLogger : RateLimitKey {
        /**
         * Shows the name of the key.
         *
         * @return `PerLogger`.
         * @since 1.0.0
         */
        override fun toString(): String = "PerLogger"
    }

    /**
     * One limit per level.
     *
     * @since 1.0.0
     */
    public object PerLevel : RateLimitKey {
        /**
         * Shows the name of the key.
         *
         * @return `PerLevel`.
         * @since 1.0.0
         */
        override fun toString(): String = "PerLevel"
    }

    /**
     * One limit per value of a field of the event or the context, such as a tenant.
     *
     * At most 4 096 values are counted at once; when more appear, all counts start anew.
     *
     * @property name the name of the field.
     * @since 1.0.0
     */
    public class PerField(public val name: String) : RateLimitKey {
        /**
         * Returns whether [other] counts the same field.
         *
         * @param other the object to compare with.
         * @return `true` if both keys name the same field.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is PerField && other.name == name

        /**
         * Returns the hash code of the name.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = name.hashCode()

        /**
         * Shows the name of the field, escaped.
         *
         * @return the key as text.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "PerField(${SafeText.text(name)})"
    }
}
