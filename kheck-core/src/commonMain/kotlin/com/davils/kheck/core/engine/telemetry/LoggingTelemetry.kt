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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kheck.core.engine.telemetry

import com.davils.kheck.api.LogLevel
import com.davils.kheck.core.Limits
import com.davils.kheck.core.LoggingEvent
import com.davils.kheck.core.LoggingEventListener
import com.davils.kheck.core.diagnostics.LogDiagnostics
import com.davils.kheck.core.diagnostics.typeOf
import com.davils.kheck.core.health.HealthRule
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.core.health.LoggingHealth
import com.davils.kheck.core.statistics.LoggingStatistics
import com.davils.kheck.core.statistics.Suppression
import com.davils.kore.core.failure.contained
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class LoggingTelemetry(
    private val intakeCapacity: Int,
    private val listeners: () -> List<LoggingEventListener>,
    private val diagnostics: () -> LogDiagnostics,
) {
    private val recorded: Map<LogLevel, AtomicLong> = RECORDED_LEVELS.associateWith { AtomicLong(0L) }
    private val suppressed: Map<Suppression, AtomicLong> = Suppression.entries.associateWith { AtomicLong(0L) }
    private val intakeQueued = AtomicLong(0L)
    private val intakeDropped = AtomicLong(0L)
    private val closedDropped = AtomicLong(0L)
    private val reentrant = AtomicLong(0L)
    private val failedListeners = AtomicLong(0L)
    private val masked = AtomicReference<Map<String, AtomicLong>>(emptyMap())
    private val sinks = AtomicReference<Map<String, SinkTally>>(emptyMap())

    fun sink(name: String): SinkTally = registered(sinks, name, ::SinkTally)

    fun retainSinks(names: Set<String>) {
        while (true) {
            val current = sinks.load()
            val kept = current.filterKeys { name -> name in names }
            if (sinks.compareAndSet(expectedValue = current, newValue = kept)) return
        }
    }

    fun accepted() {
        intakeQueued.addAndFetch(1L)
    }

    fun taken() {
        intakeQueued.addAndFetch(-1L)
    }

    fun intakeDropped() {
        intakeDropped.addAndFetch(1L)
    }

    fun displaced() {
        taken()
        intakeDropped()
    }

    fun closedDropped() {
        closedDropped.addAndFetch(1L)
    }

    fun reentrant() {
        reentrant.addAndFetch(1L)
    }

    fun processed(level: LogLevel) {
        recorded.getValue(level).addAndFetch(1L)
    }

    fun suppressed(reason: Suppression) {
        suppressed.getValue(reason).addAndFetch(1L)
    }

    fun masked(rule: String) {
        val counter = registered(masked, countedName(rule)) { AtomicLong(0L) }
        counter.addAndFetch(1L)
    }

    private fun countedName(rule: String): String {
        val current = masked.load()
        val isCounted = rule in current || current.size < Limits.MAX_COUNTED_RULES
        if (isCounted) return rule
        return OTHER_RULE
    }

    fun publish(event: LoggingEvent) {
        listeners().forEach { listener ->
            val result = contained { listener.onEvent(event) }
            result.onFailure { failure ->
                failedListeners.addAndFetch(1L)
                diagnostics().report("a listener of events failed: ${typeOf(failure)}")
            }
        }
    }

    fun statistics(): LoggingStatistics = LoggingStatistics(
        recorded = recorded.mapValues { (_, count) -> count.load() },
        suppressed = suppressed.mapValues { (_, count) -> count.load() },
        intakeQueued = intakeQueued.load().coerceAtLeast(0L),
        intakeCapacity = intakeCapacity,
        intakeDropped = intakeDropped.load(),
        droppedAfterClose = closedDropped.load(),
        reentrant = reentrant.load(),
        masked = masked.load().mapValues { (_, count) -> count.load() },
        failedListeners = failedListeners.load(),
        sinks = sinks.load().mapValues { (_, tally) -> tally.snapshot() },
    )

    fun health(rules: List<HealthRule>): LoggingHealth {
        if (rules.isEmpty()) return LoggingHealth.UP
        val snapshot = statistics()
        return LoggingHealth.worstOf(rules.map { rule -> judged(rule, snapshot) })
    }

    private fun judged(rule: HealthRule, snapshot: LoggingStatistics): LoggingHealth {
        val result = contained { rule.evaluate(snapshot) }
        return result.getOrElse { LoggingHealth(HealthStatus.Degraded, listOf(FAILED_RULE)) }
    }

    private fun <T> registered(target: AtomicReference<Map<String, T>>, name: String, create: () -> T): T {
        while (true) {
            val current = target.load()
            val known = current[name]
            if (known != null) return known
            val created = create()
            if (target.compareAndSet(expectedValue = current, newValue = current + (name to created))) return created
        }
    }

    private companion object {
        private const val OTHER_RULE: String = "other"
        private const val FAILED_RULE: String = "a health rule could not be evaluated"
        private val RECORDED_LEVELS: List<LogLevel> = LogLevel.entries - LogLevel.Off
    }
}
