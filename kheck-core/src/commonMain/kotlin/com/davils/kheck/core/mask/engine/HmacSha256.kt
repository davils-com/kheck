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

internal object HmacSha256 {
    private const val BLOCK_SIZE: Int = 64
    private const val INNER_PAD: Int = 0x36
    private const val OUTER_PAD: Int = 0x5C

    fun mac(key: ByteArray, message: ByteArray): ByteArray {
        val block = blockOf(key)
        val inner = ByteArray(BLOCK_SIZE) { index -> (block[index].toInt() xor INNER_PAD).toByte() }
        val outer = ByteArray(BLOCK_SIZE) { index -> (block[index].toInt() xor OUTER_PAD).toByte() }
        val innerDigest = Sha256.digest(inner + message)
        val result = Sha256.digest(outer + innerDigest)
        block.fill(0)
        inner.fill(0)
        outer.fill(0)
        return result
    }

    private fun blockOf(key: ByteArray): ByteArray {
        val shortened = key.takeIf { candidate -> candidate.size <= BLOCK_SIZE } ?: Sha256.digest(key)
        return shortened.copyOf(BLOCK_SIZE)
    }
}
