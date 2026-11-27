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
import com.davils.krypt.primitive.Crypto
import com.davils.krypt.primitive.key.SecretKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.io.bytestring.ByteString

/**
 * Verifies the integrity chains that sinks with `integrity { }` write, without reading any file: the application
 * reads its log and hands the lines with their seals, or the log and the text of its chain, to [verify].
 *
 * Every MAC is compared in constant time. The key and the algorithm must be those of the chain; the policy of [Crypto]
 * must permit the algorithm.
 *
 * @since 1.0.0
 */
public object IntegrityChain {
    /**
     * Verifies [entries] against the chain that starts at [anchor].
     *
     * @param crypto the cryptography that computes the MACs.
     * @param key the key of the chain.
     * @param algorithm the algorithm of the chain.
     * @param anchor the seal the chain starts from, such as [LogSeal.origin] or the last seal of a previous log.
     * @param entries the lines in the order the sink received them, with their seals.
     * @return [IntegrityReport.Intact], or [IntegrityReport.Tampered] at the first line that does not follow.
     * @since 1.0.0
     */
    public suspend fun verify(
        crypto: Crypto,
        key: SecretKey,
        algorithm: MacAlgorithm,
        anchor: LogSeal,
        entries: Flow<SealedText>,
    ): IntegrityReport {
        var previous = anchor
        var records = 0L
        var failure: IntegrityReport? = null
        val walked = entries.takeWhile { entry ->
            val expected = previous.sequence + 1
            val data = chained(previous, entry.text.encodeToByteArray())
            val isFollowing = entry.seal.sequence == expected &&
                crypto.isValid(key = key, data = data, seal = entry.seal, algorithm = algorithm)
            if (isFollowing) {
                previous = entry.seal
                records++
            }
            if (!isFollowing) failure = IntegrityReport.Tampered(expected)
            isFollowing
        }
        walked.collect()
        return failure ?: IntegrityReport.Intact(records = records, anchor = anchor, last = previous)
    }

    /**
     * Verifies [log] against [chain], the lines of [ChainLines] a sink stored next to it.
     *
     * @param crypto the cryptography that computes the MACs.
     * @param key the key of the chain.
     * @param algorithm the algorithm of the chain.
     * @param log the bytes of the log as the sink stored them, the sealed lines one after the other.
     * @param chain the lines of the chain, the anchor first, without line breaks.
     * @return the report: [IntegrityReport.Malformed] for a broken chain, [IntegrityReport.Tampered] at the first
     *   line that does not follow, [IntegrityReport.Incomplete] when the log is shorter or longer than the chain, or
     *   [IntegrityReport.Intact].
     * @since 1.0.0
     */
    public suspend fun verify(
        crypto: Crypto,
        key: SecretKey,
        algorithm: MacAlgorithm,
        log: ByteString,
        chain: List<String>,
    ): IntegrityReport {
        val lines = chain.map(ChainLines::parsed)
        val malformed = lines.indexOfFirst { line -> line == null }
        if (malformed >= 0 || lines.isEmpty()) return IntegrityReport.Malformed(line = maxOf(a = malformed, b = 0) + 1)
        val walk = Walk(crypto = crypto, key = key, algorithm = algorithm, log = log.toByteArray())
        return walk.walked(lines.filterNotNull())
    }

    internal suspend fun sealed(
        crypto: Crypto,
        key: SecretKey,
        algorithm: MacAlgorithm,
        previous: LogSeal,
        text: String,
    ): LogSeal {
        val mac = crypto.macs.mac(key, chained(previous, text.encodeToByteArray()), algorithm)
        return LogSeal(sequence = previous.sequence + 1, mac = mac)
    }

    internal fun chained(previous: LogSeal, bytes: ByteArray): ByteString =
        ByteString(previous.mac.toByteArray() + bytes)

    private suspend fun Crypto.isValid(
        key: SecretKey,
        data: ByteString,
        seal: LogSeal,
        algorithm: MacAlgorithm,
    ): Boolean = macs.verify(key = key, data = data, tag = seal.mac, algorithm = algorithm).isValid

    private class Walk(
        private val crypto: Crypto,
        private val key: SecretKey,
        private val algorithm: MacAlgorithm,
        private val log: ByteArray,
    ) {
        suspend fun walked(lines: List<ChainLine>): IntegrityReport {
            val anchor = lines.first().seal
            var previous = anchor
            var offset = 0
            for (line in lines.drop(1)) {
                val failure = failure(line = line, anchor = anchor, previous = previous, offset = offset)
                if (failure != null) return failure
                previous = line.seal
                offset += line.length
            }
            return completed(anchor, previous, offset)
        }

        private suspend fun failure(
            line: ChainLine,
            anchor: LogSeal,
            previous: LogSeal,
            offset: Int,
        ): IntegrityReport? {
            val expected = previous.sequence + 1
            val misplaced = IntegrityReport.Tampered(expected).takeIf { line.seal.sequence != expected }
            val records = previous.sequence - anchor.sequence
            val truncated = IntegrityReport.Incomplete(records).takeIf { line.length > log.size - offset }
            val structural = misplaced ?: truncated
            if (structural != null) return structural
            val bytes = log.copyOfRange(fromIndex = offset, toIndex = offset + line.length)
            val data = chained(previous, bytes)
            val isValid = crypto.isValid(key = key, data = data, seal = line.seal, algorithm = algorithm)
            return IntegrityReport.Tampered(expected).takeUnless { isValid }
        }

        private fun completed(anchor: LogSeal, last: LogSeal, offset: Int): IntegrityReport {
            val records = last.sequence - anchor.sequence
            if (offset != log.size) return IntegrityReport.Incomplete(records)
            return IntegrityReport.Intact(records = records, anchor = anchor, last = last)
        }
    }
}
