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

import com.davils.kheck.core.statistics.LoggingStatistics

/**
 * The rules of health kheck offers.
 *
 * @since 1.0.0
 */
public object HealthRules {
    private const val SATURATED: Double = 0.9
    private const val FAILING: Double = 1.0

    private val STATUSES: Map<Pair<Boolean, Boolean>, HealthStatus> = mapOf(
        Pair(first = true, second = true) to HealthStatus.Down,
        Pair(first = true, second = false) to HealthStatus.Down,
        Pair(first = false, second = true) to HealthStatus.Degraded,
        Pair(first = false, second = false) to HealthStatus.Up,
    )

    /**
     * Degraded while a sink fails.
     *
     * @since 1.0.0
     */
    public val SINKS_FAILING: HealthRule =
        threshold(name = "sinks failing", degradedAt = FAILING) { statistics -> statistics.sinksFailing.toDouble() }

    /**
     * Degraded while the intake is at least 90 % full.
     *
     * @since 1.0.0
     */
    public val INTAKE_SATURATION: HealthRule =
        threshold(name = "intake saturation", degradedAt = SATURATED) { statistics -> statistics.intakeSaturation }

    /**
     * The rules a logging uses unless it sets its own: [SINKS_FAILING] and [INTAKE_SATURATION].
     *
     * @since 1.0.0
     */
    public val DEFAULT: List<HealthRule> = listOf(SINKS_FAILING, INTAKE_SATURATION)

    /**
     * Returns a rule that is degraded when [reading] reaches [degradedAt] and down when it reaches [downAt].
     *
     * @param name what the reading is, for the reasons.
     * @param degradedAt the reading from which the logging is degraded, or `null` for never.
     * @param downAt the reading from which the logging is down, or `null` for never.
     * @param reading reads the value from the statistics.
     * @return the rule.
     * @since 1.0.0
     */
    public fun threshold(
        name: String,
        degradedAt: Double? = null,
        downAt: Double? = null,
        reading: (LoggingStatistics) -> Double,
    ): HealthRule = HealthRule { statistics ->
        judged(name = name, value = reading(statistics), degradedAt = degradedAt, downAt = downAt)
    }

    private fun judged(name: String, value: Double, degradedAt: Double?, downAt: Double?): LoggingHealth {
        val isDown = downAt != null && value >= downAt
        val isDegraded = degradedAt != null && value >= degradedAt
        val status = STATUSES.getValue(isDown to isDegraded)
        if (status == HealthStatus.Up) return LoggingHealth.UP
        return LoggingHealth(status, listOf("$name is $value"))
    }
}
