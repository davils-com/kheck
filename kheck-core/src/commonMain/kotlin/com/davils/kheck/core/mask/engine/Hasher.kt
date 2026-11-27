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

import com.davils.kheck.core.mask.MaskingConfiguration
import com.davils.krypt.core.algorithm.MacAlgorithm
import com.davils.krypt.primitive.Crypto
import com.davils.krypt.primitive.key.SecretKey
import kotlinx.io.bytestring.ByteString

internal class Hasher private constructor(
    private val crypto: Crypto,
    private val algorithm: MacAlgorithm,
    private val key: SecretKey,
) {
    suspend fun hash(value: String): String? {
        val tag = tagged(ByteString(value.encodeToByteArray())) ?: return null
        val hex = tag.toByteArray().toHex()
        return PREFIX + hex.take(VISIBLE_DIGITS)
    }

    private suspend fun tagged(data: ByteString): ByteString? {
        val current = key.takeUnless(SecretKey::isDestroyed) ?: return null
        return crypto.macs.mac(current, data, algorithm)
    }

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0')
    }

    companion object {
        private const val PREFIX: String = "hash:"
        private const val VISIBLE_DIGITS: Int = 16
        private const val BYTE_MASK: Int = 0xFF
        private const val HEX_RADIX: Int = 16

        fun of(configuration: MaskingConfiguration, crypto: Crypto): Hasher? {
            val key = configuration.hashKey ?: return null
            return Hasher(crypto = crypto, algorithm = configuration.algorithm, key = key)
        }
    }
}
