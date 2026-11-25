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

import kotlin.time.ComparableTimeMark
import kotlin.time.DurationUnit

internal class TokenBucket(
    private val capacity: Double,
    private val perSecond: Double,
    private var last: ComparableTimeMark,
) {
    private var tokens: Double = capacity

    fun take(now: ComparableTimeMark): Boolean {
        val elapsed = (now - last).toDouble(DurationUnit.SECONDS)
        last = now
        tokens = (tokens + elapsed * perSecond).coerceAtMost(capacity)
        if (tokens < 1.0) return false
        tokens -= 1.0
        return true
    }
}
