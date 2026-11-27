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

package com.davils.kheck.core.mask

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * How the masking replaces a sensitive value.
 *
 * @since 1.0.0
 */
public sealed interface Mask {
    /**
     * Replaces the whole value with [SafeText.REDACTED], which reveals neither its content nor its length.
     *
     * @since 1.0.0
     */
    public object Full : Mask {
        /**
         * Shows the name of the mask.
         *
         * @return `Full`.
         * @since 1.0.0
         */
        override fun toString(): String = "Full"
    }

    /**
     * Keeps the first [keepStart] and the last [keepEnd] characters and replaces the rest with the mask; a value too
     * short to hide at least four characters is masked whole.
     *
     * @property keepStart the characters kept at the start.
     * @property keepEnd the characters kept at the end.
     * @since 1.0.0
     */
    public class Partial(public val keepStart: Int, public val keepEnd: Int) : Mask {
        /**
         * Returns whether [other] keeps the same characters.
         *
         * @param other the object to compare with.
         * @return `true` if both masks are equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean =
            other is Partial && other.keepStart == keepStart && other.keepEnd == keepEnd

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = HASH_FACTOR * keepStart + keepEnd

        /**
         * Shows the counts of kept characters.
         *
         * @return the mask as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Partial(keepStart=$keepStart, keepEnd=$keepEnd)"

        private companion object {
            private const val HASH_FACTOR: Int = 31
        }
    }

    /**
     * Replaces the value with `hash:` and the first 16 hex digits of its MAC under the
     * [hashKey][MaskingBuilder.hashKey], HMAC-SHA-256 by default, so equal values can be correlated across records
     * without being revealed.
     *
     * @since 1.0.0
     */
    public object Hash : Mask {
        /**
         * Shows the name of the mask.
         *
         * @return `Hash`.
         * @since 1.0.0
         */
        override fun toString(): String = "Hash"
    }

    /**
     * Replaces the value with fixed text.
     *
     * @property replacement the text written instead.
     * @since 1.0.0
     */
    public class Fixed(public val replacement: String) : Mask {
        /**
         * Returns whether [other] writes the same text.
         *
         * @param other the object to compare with.
         * @return `true` if both masks are equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Fixed && other.replacement == replacement

        /**
         * Returns the hash code of the text.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = replacement.hashCode()

        /**
         * Shows the text, escaped.
         *
         * @return the mask as text.
         * @since 1.0.0
         */
        @OptIn(DavilsInternal::class)
        override fun toString(): String = "Fixed(${SafeText.quoted(replacement)})"
    }

    /**
     * Removes a field with a sensitive name entirely, and replaces a sensitive match inside a text with nothing.
     *
     * @since 1.0.0
     */
    public object Remove : Mask {
        /**
         * Shows the name of the mask.
         *
         * @return `Remove`.
         * @since 1.0.0
         */
        override fun toString(): String = "Remove"
    }
}
