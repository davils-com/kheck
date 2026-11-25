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
 * The secret key of [Mask.Hash]: the hashes of two loggings correlate only when they share the key, and nobody
 * without it can test a guess against a hash.
 *
 * The key is copied and never shown: [toString] writes only the mask. [destroy] overwrites the copy, after which the
 * hash mask of a logging that still uses the key writes the full mask instead.
 *
 * @param key the bytes of the key, copied; at least 16 bytes.
 * @throws IllegalArgumentException if [key] has fewer than 16 bytes.
 * @since 1.0.0
 */
public class MaskingKey(key: ByteArray) {
    private val bytes: ByteArray = key.copyOf()
    private var isWiped: Boolean = false

    init {
        require(key.size >= MIN_LENGTH) { "A masking key has at least $MIN_LENGTH bytes" }
    }

    /**
     * Whether [destroy] was called.
     *
     * @since 1.0.0
     */
    public val isDestroyed: Boolean get() = isWiped

    /**
     * Overwrites the key with zeros; a destroyed key hashes nothing.
     *
     * @since 1.0.0
     */
    public fun destroy() {
        bytes.fill(0)
        isWiped = true
    }

    internal fun copy(): ByteArray? {
        if (isWiped) return null
        return bytes.copyOf()
    }

    /**
     * Shows the mask, never the key.
     *
     * @return the mask.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = "MaskingKey(${SafeText.REDACTED})"

    /**
     * The creation of keys from text.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val MIN_LENGTH: Int = 16

        /**
         * Creates a key from the UTF-8 bytes of [text], such as a value from a secret store.
         *
         * @param text the key as text; at least 16 bytes in UTF-8.
         * @return the key.
         * @throws IllegalArgumentException if [text] has fewer than 16 bytes in UTF-8.
         * @since 1.0.0
         */
        public fun of(text: String): MaskingKey {
            val bytes = text.encodeToByteArray()
            val key = MaskingKey(bytes)
            bytes.fill(0)
            return key
        }
    }
}
