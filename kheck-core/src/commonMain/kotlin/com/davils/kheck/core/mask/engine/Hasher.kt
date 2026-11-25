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

package com.davils.kheck.core.mask.engine

import com.davils.kheck.core.mask.MaskingKey

internal class Hasher(private val key: MaskingKey) {
    fun hash(value: String): String? {
        val secret = key.copy() ?: return null
        val digest = HmacSha256.mac(key = secret, message = value.encodeToByteArray())
        secret.fill(0)
        return PREFIX + digest.toHex().take(VISIBLE_DIGITS)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
    }

    private companion object {
        private const val PREFIX: String = "hash:"
        private const val VISIBLE_DIGITS: Int = 16
        private const val BYTE_MASK: Int = 0xFF
        private const val HEX_RADIX: Int = 16
    }
}
