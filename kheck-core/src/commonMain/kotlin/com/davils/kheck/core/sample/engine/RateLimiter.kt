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

import com.davils.kheck.core.format.text.rendered
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.sample.RateLimit
import com.davils.kheck.core.sample.RateLimitKey
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

internal class RateLimiter(private val limit: RateLimit, private val timeSource: TimeSource.WithComparableMarks) {
    private val buckets = HashMap<String, TokenBucket>()

    private val perSecond: Double = limit.records / limit.per.toDouble(DurationUnit.SECONDS)

    fun isAllowed(record: LogRecord): Boolean {
        val now = timeSource.markNow()
        val key = keyOf(record)
        if (buckets.size >= MAX_BUCKETS && key !in buckets) buckets.clear()
        val bucket = buckets.getOrPut(key) { TokenBucket(limit.records.toDouble(), perSecond, now) }
        return bucket.take(now)
    }

    private fun keyOf(record: LogRecord): String = when (val key = limit.key) {
        RateLimitKey.Global -> ""
        RateLimitKey.PerLogger -> record.source.logger.value
        RateLimitKey.PerLevel -> record.level.name
        is RateLimitKey.PerField -> fieldKey(record, key.name)
    }

    private fun fieldKey(record: LogRecord, name: String): String {
        val value = record.fields[name] ?: record.context[name] ?: return ""
        return value.rendered()
    }

    private companion object {
        private const val MAX_BUCKETS: Int = 4_096
    }
}
