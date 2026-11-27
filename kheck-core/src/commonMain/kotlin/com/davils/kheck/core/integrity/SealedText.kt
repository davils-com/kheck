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
 * A line as a sink stored it, with its seal, to be verified by [IntegrityChain.verify].
 *
 * @property text the line exactly as the sink received it in [LogLine.text][com.davils.kheck.core.sink.LogLine.text].
 * @property seal the seal the sink received with it.
 * @since 1.0.0
 */
public class SealedText(public val text: String, public val seal: LogSeal) {
    /**
     * Compares by value: two sealed texts are equal when their texts and seals are equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [SealedText].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is SealedText && text == other.text && seal == other.seal

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * text.hashCode() + seal.hashCode()

    /**
     * Shows the seal and the length of the text, never the text.
     *
     * @return the sealed text as text.
     * @since 1.0.0
     */
    override fun toString(): String = "SealedText(seal=$seal, length=${text.length})"

    private companion object {
        private const val HASH_FACTOR: Int = 31
    }
}
