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

import kotlinx.io.bytestring.ByteString

/**
 * The text form of a chain for a sink that keeps its seals apart from its lines, such as in a second file next to its
 * log: one line per seal, `<sequence> <length> <mac>`, with the length of the sealed line in UTF-8 bytes and the MAC in
 * lower-case hexadecimal digits. The first line is the anchor, with the length 0.
 *
 * kheck writes no file; the sink stores these lines where it stores its log, and [IntegrityChain.verify] checks a log
 * against them.
 *
 * @since 1.0.0
 */
public object ChainLines {
    private const val PARTS: Int = 3
    private const val SEPARATOR: Char = ' '
    private const val HEX_RADIX: Int = 16
    private const val MAX_TAG_BYTES: Int = 64
    private const val HEX_DIGITS: String = "0123456789abcdef"

    /**
     * Writes the line of [seal] for a sealed line of [text].
     *
     * @param seal the seal of the line.
     * @param text the sealed line, as the sink received it.
     * @return the line of the chain, without a line break.
     * @since 1.0.0
     */
    public fun format(seal: LogSeal, text: String): String =
        "${seal.sequence}$SEPARATOR${text.encodeToByteArray().size}$SEPARATOR${seal.hex}"

    /**
     * Writes the first line of a chain, for the seal it starts from.
     *
     * @param anchor the seal the chain starts from, such as [LogSeal.origin].
     * @return the line of the chain, without a line break.
     * @since 1.0.0
     */
    public fun anchor(anchor: LogSeal): String = "${anchor.sequence}${SEPARATOR}0$SEPARATOR${anchor.hex}"

    internal fun parsed(line: String): ChainLine? {
        val parts = line.split(SEPARATOR)
        val sequence = parts.numberAt(0)?.takeIf { number -> number >= 0L }
        val length = parts.numberAt(1)?.takeIf { number -> number in 0..Int.MAX_VALUE }
        val mac = parts.getOrNull(2)?.let(::bytesOf)
        val seal = sealOf(sequence, mac)
        if (parts.size != PARTS || seal == null || length == null) return null
        return ChainLine(seal = seal, length = length.toInt())
    }

    private fun List<String>.numberAt(index: Int): Long? = getOrNull(index)?.toLongOrNull()

    private fun sealOf(sequence: Long?, mac: ByteString?): LogSeal? {
        if (sequence == null || mac == null) return null
        return LogSeal(sequence = sequence, mac = mac)
    }

    private fun bytesOf(hex: String): ByteString? {
        val isSized = hex.length % 2 == 0 && hex.length in 2..MAX_TAG_BYTES * 2
        val isHex = isSized && hex.all { digit -> digit in HEX_DIGITS }
        if (!isHex) return null
        val bytes = ByteArray(hex.length / 2) { index -> byteAt(hex, index) }
        return ByteString(bytes)
    }

    private fun byteAt(hex: String, index: Int): Byte {
        val digits = hex.substring(startIndex = index * 2, endIndex = index * 2 + 2)
        return digits.toInt(HEX_RADIX).toByte()
    }
}
