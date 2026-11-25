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

package com.davils.kheck.core.sample.engine

import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.sample.SamplingConfiguration
import kotlin.math.floor
import kotlin.time.TimeSource

internal class Sampler(
    private val configuration: SamplingConfiguration,
    timeSource: TimeSource.WithComparableMarks,
) {
    private val deduplicator = configuration.deduplication?.let { value -> Deduplicator(value, timeSource) }

    private val limiter = configuration.limit?.let { value -> RateLimiter(value, timeSource) }

    private var seen: Long = 0L

    fun decide(record: LogRecord): SamplingDecision {
        if (deduplicator?.isDuplicate(record) == true) return SamplingDecision.Deduplicated
        return admitted(record)
    }

    private fun admitted(record: LogRecord): SamplingDecision {
        if (record.level.isAtLeast(configuration.alwaysFrom)) return SamplingDecision.Keep
        return limited(record) ?: sampled()
    }

    private fun limited(record: LogRecord): SamplingDecision? {
        if (limiter?.isAllowed(record) != false) return null
        return SamplingDecision.RateLimited
    }

    private fun sampled(): SamplingDecision {
        val before = kept(seen)
        seen += 1
        val isKept = configuration.rate >= 1.0 || kept(seen) > before
        if (isKept) return SamplingDecision.Keep
        return SamplingDecision.Sampled
    }

    private fun kept(count: Long): Long = floor(count * configuration.rate).toLong()

    fun expired(): List<LogRecord> = deduplicator?.expired().orEmpty()
}
