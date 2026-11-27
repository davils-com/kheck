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

package com.davils.kheck.core.integrity

/**
 * The result of verifying an integrity chain.
 *
 * @since 1.0.0
 */
public sealed interface IntegrityReport {
    /**
     * Every line is present, unchanged and in order.
     *
     * @property records the lines verified after the anchor.
     * @property anchor the seal the chain started from.
     * @property last the seal of the last line, from which the chain goes on.
     * @since 1.0.0
     */
    public class Intact(public val records: Long, public val anchor: LogSeal, public val last: LogSeal) :
        IntegrityReport {
        private val values: List<Any> get() = listOf(records, anchor, last)

        /**
         * Compares by value.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Intact].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Intact && values == other.values

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = values.hashCode()

        /**
         * Shows the count and the seals.
         *
         * @return the report as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Intact(records=$records, anchor=$anchor, last=$last)"
    }

    /**
     * The line with [sequence] was changed, removed, added or moved, or its seal does not match.
     *
     * @property sequence the sequence the chain expected at the first line that does not match.
     * @since 1.0.0
     */
    public class Tampered(public val sequence: Long) : IntegrityReport {
        /**
         * Compares by value.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Tampered].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Tampered && sequence == other.sequence

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = sequence.hashCode()

        /**
         * Shows the sequence.
         *
         * @return the report as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Tampered(sequence=$sequence)"
    }

    /**
     * The log ends before the chain does, or goes on after it: lines are missing at the end or were added.
     *
     * @property records the lines verified before the log and the chain parted.
     * @since 1.0.0
     */
    public class Incomplete(public val records: Long) : IntegrityReport {
        /**
         * Compares by value.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Incomplete].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Incomplete && records == other.records

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = records.hashCode()

        /**
         * Shows the count.
         *
         * @return the report as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Incomplete(records=$records)"
    }

    /**
     * A line of the chain is not in the form of [ChainLines], or the chain has no anchor.
     *
     * @property line the number of the malformed line of the chain, from 1.
     * @since 1.0.0
     */
    public class Malformed(public val line: Int) : IntegrityReport {
        /**
         * Compares by value.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [Malformed].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Malformed && line == other.line

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = line

        /**
         * Shows the line.
         *
         * @return the report as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Malformed(line=$line)"
    }
}
