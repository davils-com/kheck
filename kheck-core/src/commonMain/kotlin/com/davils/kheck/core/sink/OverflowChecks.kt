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

package com.davils.kheck.core.sink

import com.davils.kheck.core.Limits
import com.davils.kheck.core.LoggingViolationCodes
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty

internal fun ValidationScope.checkOverflow(property: KProperty<*>, overflow: LogOverflow) {
    val block = overflow as? LogOverflow.Block ?: return
    val isPositive = block.maxWait.isPositive() && block.maxWait.isFinite()
    if (isPositive) return
    fail(
        property,
        "must wait a finite, positive time, but waits ${block.maxWait}",
        LoggingViolationCodes.INVALID_WAIT,
    )
}

internal fun ValidationScope.checkCapacity(property: KProperty<*>, capacity: Int) {
    if (capacity in 1..Limits.MAX_CAPACITY) return
    fail(
        property,
        "must hold between 1 and ${Limits.MAX_CAPACITY} records, but holds $capacity",
        LoggingViolationCodes.INVALID_CAPACITY,
    )
}

internal fun ValidationScope.checkRetry(property: KProperty<*>, retry: SinkRetry) {
    val isAttemptsValid = retry.attempts in 1..MAX_ATTEMPTS
    val isBackoffValid = !retry.backoff.isNegative() && retry.backoff <= retry.maxBackoff && retry.maxBackoff.isFinite()
    if (isAttemptsValid && isBackoffValid) return
    fail(
        property,
        "must try between 1 and $MAX_ATTEMPTS times and wait no negative time up to a finite most",
        LoggingViolationCodes.INVALID_LIMIT,
    )
}

private const val MAX_ATTEMPTS: Int = 100
