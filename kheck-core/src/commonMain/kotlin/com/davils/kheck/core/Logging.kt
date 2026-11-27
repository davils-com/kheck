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

package com.davils.kheck.core

import com.davils.kheck.api.Logger
import com.davils.kheck.api.LoggerName
import com.davils.kheck.core.engine.LoggingEngine
import com.davils.kheck.core.engine.telemetry.MeterBinding
import com.davils.kheck.core.health.HealthStatus
import com.davils.kheck.core.health.LoggingHealth
import com.davils.kheck.core.statistics.LoggingStatistics
import com.davils.kollect.core.MeterRegistry
import com.davils.kollect.core.health.Health
import com.davils.kollect.core.health.HealthIndicator
import com.davils.kollect.core.health.ReasonCode
import com.davils.kore.core.validation.report.ValidationReport
import com.davils.kore.core.validation.validate
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * A running logging: it hands out loggers, takes their records through its pipeline and writes them to its sinks.
 *
 * A call of a logger puts the record into the intake and returns; the coroutine of the logging, started by [logging]
 * on the scope and dispatcher of the application, filters, samples, enriches and masks it, and each sink writes it on
 * a coroutine of its own. Nothing of a record can make the call of a logger fail.
 *
 * Every member is safe to call from any thread. [close] flushes and closes every sink; a logging must be closed, or
 * the records in its queues are lost when the scope ends. kheck registers no shutdown hook: the application closes the
 * logging at its end, directly, with a timeout, or as the last task of a knot shutdown through `kheck-knot`.
 *
 * @since 1.0.0
 */
public class Logging internal constructor(private val engine: LoggingEngine) {
    /**
     * The configuration in effect.
     *
     * @since 1.0.0
     */
    public val configuration: LoggingConfiguration get() = engine.configuration

    /**
     * The overrides of thresholds that apply while the logging runs.
     *
     * @since 1.0.0
     */
    public val levels: LogLevels get() = engine.levels

    /**
     * Whether [close] was called.
     *
     * @since 1.0.0
     */
    public val isClosed: Boolean get() = engine.isClosed

    /**
     * Returns the logger of [name]; the same name returns the same logger.
     *
     * @param name the dotted name; empty for the root.
     * @return the logger.
     * @throws IllegalArgumentException if [name] is longer than the limit of a name.
     * @since 1.0.0
     */
    public fun logger(name: String): Logger = engine.logger(LoggerName(name))

    /**
     * Returns the logger named after [type], as [LoggerName.of] names it.
     *
     * @param type the class that logs.
     * @return the logger.
     * @since 1.0.0
     */
    public fun logger(type: KClass<*>): Logger = engine.logger(LoggerName.of(type))

    /**
     * Returns a snapshot of what the logging did since it started.
     *
     * @return the statistics.
     * @since 1.0.0
     */
    public fun statistics(): LoggingStatistics = engine.telemetry.statistics()

    /**
     * Judges the health of the logging by its [rules][LoggingConfiguration.healthRules].
     *
     * @return the health.
     * @since 1.0.0
     */
    public fun health(): LoggingHealth = engine.telemetry.health(engine.configuration.healthRules)

    /**
     * Declares the statistics of this logging as meters of [registry], as [LoggingMetricNames] names them, and a
     * timer of the writes of every sink.
     *
     * Every meter carries [labels] first, such as the service or the instance, before its own label; kollect has no
     * labels of a registry. Binding twice to one registry shares the families of the first binding.
     *
     * @param registry the registry of the application.
     * @param labels the labels of every meter, in order; their keys follow the grammar of kollect.
     * @return the meters; closing them removes them from [registry].
     * @throws IllegalArgumentException if a key of [labels] is no valid label key of kollect.
     * @since 1.0.0
     */
    public fun bindTo(registry: MeterRegistry, labels: Map<String, String> = emptyMap()): LoggingMeters {
        val binding = MeterBinding(registry = registry, labels = labels, read = ::statistics)
        return LoggingMeters(binding.bound(engine.telemetry))
    }

    /**
     * Returns the health of this logging as a health indicator of kollect: degraded or down with the
     * [codes][LoggingHealth.codes] of its reasons, such as [ReasonCode.ThresholdCrossed] for a crossed threshold and
     * [ReasonCode.CheckFailed] for a rule that threw.
     *
     * @return the indicator, which evaluates the rules on every check.
     * @since 1.0.0
     */
    public fun healthIndicator(): HealthIndicator = HealthIndicator { indicated(health()) }

    /**
     * Suspends until every record accepted before the call is written and every sink is flushed.
     *
     * Returns at once when the logging is closed.
     *
     * @since 1.0.0
     */
    public suspend fun flush() {
        engine.flush()
    }

    /**
     * Flushes as [flush] does, but waits at most [timeout].
     *
     * @param timeout the longest wait.
     * @return whether every record accepted before the call was written and every sink flushed in time; `true` when the
     *   logging is closed.
     * @since 1.0.0
     */
    public suspend fun flush(timeout: Duration): Boolean = engine.flush(timeout)

    /**
     * Replaces the configuration while the logging runs; the intake cannot change.
     *
     * The records accepted before are written with the old configuration, whose sinks are flushed and closed; the
     * sinks of the new one are opened. A refused configuration changes nothing and is announced as an event.
     *
     * @param block configures the new configuration from scratch.
     * @return whether the configuration was applied.
     * @since 1.0.0
     */
    public suspend fun reconfigure(block: LoggingBuilder.() -> Unit): Reconfiguration {
        val builder = LoggingBuilder()
        builder.block()
        val report = builder.validate()
        if (!report.isValid) return rejected(report)
        return compatible(builder.build())
    }

    /**
     * Writes every waiting record, flushes and closes every sink and stops the coroutines of the logging.
     *
     * Records logged afterwards are dropped and counted. Calling it again waits for the first call to finish.
     *
     * @since 1.0.0
     */
    public suspend fun close() {
        engine.close()
    }

    /**
     * Closes the logging as [close] does, but waits at most [timeout]: when the sinks do not finish in time, the
     * coroutines of the logging are cancelled and the records still waiting are dropped and counted.
     *
     * Use it at the end of a process, where a sink that hangs must not keep the process alive.
     *
     * @param timeout the longest wait; zero or negative cancels at once.
     * @return [Closure.Completed], or [Closure.TimedOut] with the records that were lost.
     * @since 1.0.0
     */
    public suspend fun close(timeout: Duration): Closure = engine.close(timeout)

    /**
     * Shows the names of the sinks and whether the logging is closed.
     *
     * @return the logging as text.
     * @since 1.0.0
     */
    override fun toString(): String = "Logging(sinks=${configuration.sinks.keys}, isClosed=$isClosed)"

    private suspend fun compatible(configuration: LoggingConfiguration): Reconfiguration {
        val compatibility = IntakeCompatibility(active = engine.configuration.intake, intake = configuration.intake)
        val report = compatibility.validate(SUBJECT)
        if (!report.isValid) return rejected(report)
        return engine.reconfigure(configuration)
    }

    private fun rejected(report: ValidationReport): Reconfiguration {
        engine.telemetry.publish(LoggingEvent.ConfigurationRejected(report))
        return Reconfiguration.Rejected(report)
    }

    @Suppress("SpreadOperator")
    private fun indicated(health: LoggingHealth): Health {
        val codes = health.codes.distinct().toTypedArray()
        return when (health.status) {
            HealthStatus.Up -> Health.Up
            HealthStatus.Degraded -> Health.degraded(*codes)
            HealthStatus.Down -> Health.down(*codes)
        }
    }

    private companion object {
        private const val SUBJECT: String = "logging"
    }
}
