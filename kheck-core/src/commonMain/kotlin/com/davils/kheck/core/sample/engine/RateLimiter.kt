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
import com.davils.kontinuity.core.environment
import com.davils.kontinuity.ratelimiter.KeyedRateLimiter
import com.davils.kontinuity.ratelimiter.keyedRateLimiter
import com.davils.kontinuity.ratelimiter.tokenBucket
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val LIMITER_NAME: String = "kheck-sampling"
private const val MAX_KEYS: Int = 4_096

internal class RateLimiter(limit: RateLimit, timeSource: TimeSource.WithComparableMarks) {
    private val key: RateLimitKey = limit.key

    private val keyed: KeyedRateLimiter<String> = keyedRateLimiter(
        name = LIMITER_NAME,
        maxKeys = MAX_KEYS,
        idleTimeout = idleTimeoutOf(limit.per),
        environment = environment { this.timeSource = timeSource },
    ) {
        limitForPeriod = limit.records
        limitRefreshPeriod = limit.per
        timeout = Duration.ZERO
        algorithm = tokenBucket(burst = limit.records)
    }

    fun isAllowed(record: LogRecord): Boolean = keyed.tryAcquire(keyOf(record))

    private fun keyOf(record: LogRecord): String = when (val chosen = key) {
        RateLimitKey.Global -> ""
        RateLimitKey.PerLogger -> record.source.logger.value
        RateLimitKey.PerLevel -> record.level.name
        is RateLimitKey.PerField -> fieldKey(record, chosen.name)
    }

    private fun fieldKey(record: LogRecord, name: String): String {
        val value = record.fields[name] ?: record.context[name] ?: return ""
        return value.rendered()
    }

    private companion object {
        fun idleTimeoutOf(period: Duration): Duration {
            val idle = maxOf(a = period, b = KeyedRateLimiter.DEFAULT_IDLE_TIMEOUT)
            return idle.coerceAtMost(KeyedRateLimiter.HARD_IDLE_TIMEOUT)
        }
    }
}
