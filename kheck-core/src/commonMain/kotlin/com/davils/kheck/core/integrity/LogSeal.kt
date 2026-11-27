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

import com.davils.krypt.core.algorithm.MacAlgorithm
import kotlinx.io.bytestring.ByteString

/**
 * The seal of one line of a sink in its integrity chain: the number of the line in the chain and the MAC over the MAC
 * of the line before and the text of this line, so a changed, removed, added or moved line breaks every later seal.
 *
 * A seal is no secret: without the key nobody can forge it. The seal of the last line written is the anchor from which
 * a chain goes on after a restart.
 *
 * @property sequence the number of the line in the chain; the origin has 0, the first line 1.
 * @property mac the MAC of the line, as long as a tag of the algorithm of the chain.
 * @since 1.0.0
 */
public class LogSeal(public val sequence: Long, public val mac: ByteString) {
    /**
     * The MAC as lower-case hexadecimal digits.
     *
     * @since 1.0.0
     */
    public val hex: String get() = mac.toByteArray().joinToString(separator = "") { byte -> byte.hex() }

    /**
     * Compares by value: two seals are equal when their sequences and MACs are equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [LogSeal].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is LogSeal && sequence == other.sequence && mac == other.mac

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * sequence.hashCode() + mac.hashCode()

    /**
     * Shows the sequence and the MAC in hexadecimal digits.
     *
     * @return the seal as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LogSeal(sequence=$sequence, mac=$hex)"

    /**
     * The origin of a chain.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val HASH_FACTOR: Int = 31
        private const val BYTE_MASK: Int = 0xFF
        private const val HEX_RADIX: Int = 16

        /**
         * Returns the origin of a chain under [algorithm]: sequence 0 and a MAC of zero bytes, as long as a tag.
         *
         * @param algorithm the algorithm of the chain.
         * @return the origin.
         * @since 1.0.0
         */
        public fun origin(algorithm: MacAlgorithm): LogSeal =
            LogSeal(sequence = 0L, mac = ByteString(ByteArray(algorithm.tagBytes)))

        private fun Byte.hex(): String = (toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
    }
}
