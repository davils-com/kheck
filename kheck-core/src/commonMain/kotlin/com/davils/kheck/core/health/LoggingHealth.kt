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

package com.davils.kheck.core.health

import com.davils.kollect.core.health.ReasonCode
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * The health of a logging: the worst status of its rules, and why.
 *
 * @property status the status.
 * @param reasons why the status is not [HealthStatus.Up], one per rule that is not satisfied, copied; none by default.
 * @param codes the kinds of the [reasons], one per reason, copied; [ReasonCode.Reported] for every reason by default.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LoggingHealth(
    public val status: HealthStatus,
    reasons: List<String> = emptyList(),
    codes: List<ReasonCode> = reasons.map { ReasonCode.Reported },
) {
    /**
     * Why the status is not [HealthStatus.Up]; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val reasons: List<String> = reasons.immutableList()

    /**
     * The kinds of the reasons, as kollect names them, such as [ReasonCode.ThresholdCrossed]; the list cannot be
     * changed.
     *
     * @since 1.0.0
     */
    public val codes: List<ReasonCode> = codes.immutableList()

    private val values: List<Any?> get() = listOf(status, reasons, codes)

    /**
     * Compares by value: two [LoggingHealth] values are equal when every property is equal.
     *
     * @param other the object to compare with.
     * @return whether [other] is an equal [LoggingHealth].
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean = other is LoggingHealth && values == other.values

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = values.hashCode()

    /**
     * Shows the status and the reasons.
     *
     * @return the health as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LoggingHealth(status=$status, reasons=$reasons)"

    /**
     * The healthy state and the combination of results.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Healthy, without reasons.
         *
         * @since 1.0.0
         */
        public val UP: LoggingHealth = LoggingHealth(HealthStatus.Up)

        /**
         * Returns the worst status of [results] with the reasons of all of them; [UP] for none.
         *
         * @param results the results of the rules.
         * @return the combined health.
         * @since 1.0.0
         */
        public fun worstOf(results: List<LoggingHealth>): LoggingHealth {
            val worst = results.maxOfOrNull { result -> result.status } ?: return UP
            return LoggingHealth(
                status = worst,
                reasons = results.flatMap { result -> result.reasons },
                codes = results.flatMap { result -> result.codes },
            )
        }
    }
}
