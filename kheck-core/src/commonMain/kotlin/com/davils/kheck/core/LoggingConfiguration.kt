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

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.internal.orderedCopy
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.diagnostics.LogDiagnostics
import com.davils.kheck.core.filter.LogFilter
import com.davils.kheck.core.health.HealthRule
import com.davils.kheck.core.intake.IntakeBuilder
import com.davils.kheck.core.intake.IntakeConfiguration
import com.davils.kheck.core.mask.MaskingBuilder
import com.davils.kheck.core.mask.MaskingConfiguration
import com.davils.kheck.core.route.LoggerConfiguration
import com.davils.kheck.core.sample.SamplingBuilder
import com.davils.kheck.core.sample.SamplingConfiguration
import com.davils.kheck.core.sink.LogSinkDefinition
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlin.reflect.KProperty
import kotlin.time.Clock
import kotlin.time.TimeSource

/**
 * The configuration of a logging, as [LoggingBuilder] describes it.
 *
 * It is checked as a whole: every logger and fallback names a declared sink, no sink falls back to itself, and the
 * counts of loggers, sinks and filters stay within [Limits.MAX_DECLARATIONS]. A configuration without sinks or with
 * an exclusive sink that no logger routes to is valid, with a warning.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LoggingConfiguration internal constructor(builder: LoggingBuilder) : Validatable {
    /**
     * The threshold of every logger without one of its own.
     *
     * @since 1.0.0
     */
    public val level: LogLevel = builder.level

    /**
     * The clock of the timestamps.
     *
     * @since 1.0.0
     */
    public val clock: Clock = builder.clock

    /**
     * The time source of durations, windows and rate limits.
     *
     * @since 1.0.0
     */
    public val timeSource: TimeSource.WithComparableMarks = builder.timeSource

    /**
     * Where the logging reports its own trouble.
     *
     * @since 1.0.0
     */
    public val diagnostics: LogDiagnostics = builder.diagnostics

    /**
     * The rules of the health; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val healthRules: List<HealthRule> = builder.healthRules.immutableList()

    /**
     * The configured loggers, per dotted name; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val loggers: Map<String, LoggerConfiguration> = builder.loggers.orderedCopy()

    /**
     * The declared sinks, per name; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val sinks: Map<String, LogSinkDefinition> = builder.sinks.orderedCopy()

    /**
     * The filters every record must pass; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val filters: List<LogFilter> = builder.filters.immutableList()

    /**
     * The listeners of the events; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val listeners: List<LoggingEventListener> = builder.listeners.immutableList()

    /**
     * The intake.
     *
     * @since 1.0.0
     */
    public val intake: IntakeConfiguration = builder.intake ?: IntakeBuilder().build()

    /**
     * The masking.
     *
     * @since 1.0.0
     */
    public val masking: MaskingConfiguration = builder.masking ?: MaskingBuilder().build()

    /**
     * The sampling.
     *
     * @since 1.0.0
     */
    public val sampling: SamplingConfiguration = builder.sampling ?: SamplingBuilder().build()

    /**
     * The fields every record carries in its context; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val enrichment: Map<String, LogValue> = builder.enrichment.orderedCopy()

    /**
     * Checks the sinks, the routes, the fallbacks and the counts.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        checkCount(::loggers, loggers.size)
        checkCount(::sinks, sinks.size)
        checkCount(::filters, filters.size)
        val isSilent = sinks.isEmpty() && level != LogLevel.Off
        if (isSilent) warn(::sinks, "declares no sink, so no record is written", LoggingViolationCodes.NO_SINKS)
        each(::sinks, sinks) { definition -> checkSink(definition) }
        each(::loggers, loggers) { logger -> checkRoutes(logger) }
    }

    /**
     * Shows the level, the names of loggers and sinks, and the parts of the pipeline.
     *
     * @return the configuration as text.
     * @since 1.0.0
     */
    override fun toString(): String =
        "LoggingConfiguration(level=$level, loggers=${loggers.keys}, sinks=${sinks.keys}, intake=$intake, " +
            "masking=$masking, sampling=$sampling, enrichment=${enrichment.keys})"

    private fun ValidationScope.checkCount(property: KProperty<*>, count: Int) {
        if (count <= Limits.MAX_DECLARATIONS) return
        fail(property, "must not declare more than ${Limits.MAX_DECLARATIONS}", LoggingViolationCodes.TOO_MANY)
    }

    private fun ValidationScope.checkSink(definition: LogSinkDefinition) {
        if (definition.name.isBlank()) {
            fail(LogSinkDefinition::name, "must not be blank", LoggingViolationCodes.BLANK_NAME)
        }
        checkFallback(definition)
        val isRouted = loggers.values.any { logger -> definition.name in logger.sinks }
        val isUnrouted = definition.options.isExclusive && !isRouted
        if (!isUnrouted) return
        warn(LogSinkDefinition::options, "is exclusive but no logger routes to it", LoggingViolationCodes.UNROUTED_SINK)
    }

    private fun ValidationScope.checkFallback(definition: LogSinkDefinition) {
        val fallback = definition.options.fallback ?: return
        if (fallback == definition.name) {
            fail(LogSinkDefinition::options, "must not fall back to itself", LoggingViolationCodes.SELF_FALLBACK)
        }
        if (fallback in sinks) return
        fail(
            LogSinkDefinition::options,
            "falls back to a sink that is not declared",
            LoggingViolationCodes.UNKNOWN_SINK,
        )
    }

    private fun ValidationScope.checkRoutes(logger: LoggerConfiguration) {
        val unknown = logger.sinks.filterNot { name -> name in sinks }
        if (unknown.isEmpty()) return
        fail(
            LoggerConfiguration::sinks,
            "routes to ${unknown.size} sinks that are not declared",
            LoggingViolationCodes.UNKNOWN_SINK,
        )
    }
}
