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

internal object Sha256 {
    private const val BLOCK_SIZE: Int = 64
    private const val ROUNDS: Int = 64
    private const val LENGTH_BYTES: Int = 8
    private const val BYTE_MASK: Int = 0xFF
    private const val BITS_PER_BYTE: Int = 8
    private const val PADDING_BYTE: Int = 0x80
    private const val WORD_BYTES: Int = 4
    private const val HASH_WORDS: Int = 8
    private const val MESSAGE_WORDS: Int = 16
    private const val FIRST_BYTE_SHIFT: Int = 24
    private const val SECOND_BYTE_SHIFT: Int = 16
    private const val THIRD_BYTE_SHIFT: Int = 8
    private const val SECOND_BYTE: Int = 1
    private const val THIRD_BYTE: Int = 2
    private const val FOURTH_BYTE: Int = 3
    private const val EARLY_LAG: Int = 15
    private const val LATE_LAG: Int = 2
    private const val FAR_LAG: Int = 16
    private const val NEAR_LAG: Int = 7
    private const val SIGMA0_FIRST: Int = 7
    private const val SIGMA0_SECOND: Int = 18
    private const val SIGMA0_SHIFT: Int = 3
    private const val SIGMA1_FIRST: Int = 17
    private const val SIGMA1_SECOND: Int = 19
    private const val SIGMA1_SHIFT: Int = 10
    private const val SUM0_FIRST: Int = 2
    private const val SUM0_SECOND: Int = 13
    private const val SUM0_THIRD: Int = 22
    private const val SUM1_FIRST: Int = 6
    private const val SUM1_SECOND: Int = 11
    private const val SUM1_THIRD: Int = 25
    private const val A: Int = 0
    private const val B: Int = 1
    private const val C: Int = 2
    private const val D: Int = 3
    private const val E: Int = 4
    private const val F: Int = 5
    private const val G: Int = 6
    private const val H: Int = 7

    private val ROUND_CONSTANTS: IntArray = intArrayOf(
        0x428a2f98.toInt(),
        0x71374491.toInt(),
        0xb5c0fbcf.toInt(),
        0xe9b5dba5.toInt(),
        0x3956c25b.toInt(),
        0x59f111f1.toInt(),
        0x923f82a4.toInt(),
        0xab1c5ed5.toInt(),
        0xd807aa98.toInt(),
        0x12835b01.toInt(),
        0x243185be.toInt(),
        0x550c7dc3.toInt(),
        0x72be5d74.toInt(),
        0x80deb1fe.toInt(),
        0x9bdc06a7.toInt(),
        0xc19bf174.toInt(),
        0xe49b69c1.toInt(),
        0xefbe4786.toInt(),
        0x0fc19dc6.toInt(),
        0x240ca1cc.toInt(),
        0x2de92c6f.toInt(),
        0x4a7484aa.toInt(),
        0x5cb0a9dc.toInt(),
        0x76f988da.toInt(),
        0x983e5152.toInt(),
        0xa831c66d.toInt(),
        0xb00327c8.toInt(),
        0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(),
        0xd5a79147.toInt(),
        0x06ca6351.toInt(),
        0x14292967.toInt(),
        0x27b70a85.toInt(),
        0x2e1b2138.toInt(),
        0x4d2c6dfc.toInt(),
        0x53380d13.toInt(),
        0x650a7354.toInt(),
        0x766a0abb.toInt(),
        0x81c2c92e.toInt(),
        0x92722c85.toInt(),
        0xa2bfe8a1.toInt(),
        0xa81a664b.toInt(),
        0xc24b8b70.toInt(),
        0xc76c51a3.toInt(),
        0xd192e819.toInt(),
        0xd6990624.toInt(),
        0xf40e3585.toInt(),
        0x106aa070.toInt(),
        0x19a4c116.toInt(),
        0x1e376c08.toInt(),
        0x2748774c.toInt(),
        0x34b0bcb5.toInt(),
        0x391c0cb3.toInt(),
        0x4ed8aa4a.toInt(),
        0x5b9cca4f.toInt(),
        0x682e6ff3.toInt(),
        0x748f82ee.toInt(),
        0x78a5636f.toInt(),
        0x84c87814.toInt(),
        0x8cc70208.toInt(),
        0x90befffa.toInt(),
        0xa4506ceb.toInt(),
        0xbef9a3f7.toInt(),
        0xc67178f2.toInt(),
    )

    private val INITIAL: IntArray = intArrayOf(
        0x6a09e667.toInt(),
        0xbb67ae85.toInt(),
        0x3c6ef372.toInt(),
        0xa54ff53a.toInt(),
        0x510e527f.toInt(),
        0x9b05688c.toInt(),
        0x1f83d9ab.toInt(),
        0x5be0cd19.toInt(),
    )

    fun digest(message: ByteArray): ByteArray {
        val state = INITIAL.copyOf()
        val padded = padded(message)
        val words = IntArray(ROUNDS)
        for (offset in padded.indices step BLOCK_SIZE) {
            compress(state = state, block = padded, offset = offset, words = words)
        }
        val result = ByteArray(HASH_WORDS * WORD_BYTES)
        state.forEachIndexed { index, word -> writeWord(result, index * WORD_BYTES, word) }
        return result
    }

    private fun padded(message: ByteArray): ByteArray {
        val bitLength = message.size.toLong() * BITS_PER_BYTE
        val withMarker = message.size + 1
        val zeros = (BLOCK_SIZE - (withMarker + LENGTH_BYTES) % BLOCK_SIZE) % BLOCK_SIZE
        val result = message.copyOf(withMarker + zeros + LENGTH_BYTES)
        result[message.size] = PADDING_BYTE.toByte()
        for (index in 0 until LENGTH_BYTES) {
            val shift = (LENGTH_BYTES - 1 - index) * BITS_PER_BYTE
            result[result.size - LENGTH_BYTES + index] = (bitLength ushr shift).toByte()
        }
        return result
    }

    private fun compress(state: IntArray, block: ByteArray, offset: Int, words: IntArray) {
        for (index in 0 until MESSAGE_WORDS) words[index] = readWord(block, offset + index * WORD_BYTES)
        for (index in MESSAGE_WORDS until ROUNDS) words[index] = expanded(words, index)
        val working = state.copyOf()
        for (index in 0 until ROUNDS) round(working, words[index] + ROUND_CONSTANTS[index])
        for (index in state.indices) state[index] += working[index]
    }

    private fun expanded(words: IntArray, index: Int): Int {
        val early = words[index - EARLY_LAG]
        val late = words[index - LATE_LAG]
        val sigma0 = early.rotateRight(SIGMA0_FIRST) xor early.rotateRight(SIGMA0_SECOND) xor (early ushr SIGMA0_SHIFT)
        val sigma1 = late.rotateRight(SIGMA1_FIRST) xor late.rotateRight(SIGMA1_SECOND) xor (late ushr SIGMA1_SHIFT)
        return words[index - FAR_LAG] + sigma0 + words[index - NEAR_LAG] + sigma1
    }

    private fun round(working: IntArray, scheduled: Int) {
        val a = working[A]
        val e = working[E]
        val sum1 = e.rotateRight(SUM1_FIRST) xor e.rotateRight(SUM1_SECOND) xor e.rotateRight(SUM1_THIRD)
        val choice = e and working[F] xor (e.inv() and working[G])
        val first = working[H] + sum1 + choice + scheduled
        val sum0 = a.rotateRight(SUM0_FIRST) xor a.rotateRight(SUM0_SECOND) xor a.rotateRight(SUM0_THIRD)
        val majority = a and working[B] xor (a and working[C]) xor (working[B] and working[C])
        val second = sum0 + majority
        working[H] = working[G]
        working[G] = working[F]
        working[F] = e
        working[E] = working[D] + first
        working[D] = working[C]
        working[C] = working[B]
        working[B] = a
        working[A] = first + second
    }

    private fun readWord(bytes: ByteArray, offset: Int): Int {
        val first = bytes[offset].toInt() and BYTE_MASK shl FIRST_BYTE_SHIFT
        val second = bytes[offset + SECOND_BYTE].toInt() and BYTE_MASK shl SECOND_BYTE_SHIFT
        val third = bytes[offset + THIRD_BYTE].toInt() and BYTE_MASK shl THIRD_BYTE_SHIFT
        val fourth = bytes[offset + FOURTH_BYTE].toInt() and BYTE_MASK
        return first or second or third or fourth
    }

    private fun writeWord(bytes: ByteArray, offset: Int, word: Int) {
        bytes[offset] = (word ushr FIRST_BYTE_SHIFT).toByte()
        bytes[offset + SECOND_BYTE] = (word ushr SECOND_BYTE_SHIFT).toByte()
        bytes[offset + THIRD_BYTE] = (word ushr THIRD_BYTE_SHIFT).toByte()
        bytes[offset + FOURTH_BYTE] = word.toByte()
    }
}
