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
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.diagnostics.LogDiagnostics
import com.davils.kheck.core.filter.LogFilter
import com.davils.kheck.core.health.HealthRule
import com.davils.kheck.core.health.HealthRules
import com.davils.kheck.core.intake.IntakeBuilder
import com.davils.kheck.core.intake.IntakeConfiguration
import com.davils.kheck.core.mask.MaskingBuilder
import com.davils.kheck.core.mask.MaskingConfiguration
import com.davils.kheck.core.route.LoggerBuilder
import com.davils.kheck.core.route.LoggerConfiguration
import com.davils.kheck.core.sample.SamplingBuilder
import com.davils.kheck.core.sample.SamplingConfiguration
import com.davils.kheck.core.sink.LogSink
import com.davils.kheck.core.sink.LogSinkBuilder
import com.davils.kheck.core.sink.LogSinkConfiguration
import com.davils.kheck.core.sink.LogSinkDefinition
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.kore.core.processEnvironment
import kotlin.time.Clock
import kotlin.time.TimeSource

/**
 * Configures a logging inside [logging] or [Logging.reconfigure].
 *
 * Sinks are declared with `console { }`, `sink("name", sink) { }` and the sinks of other modules; loggers with
 * [logger]; the pipeline with [intake], [mask], [sample], [enrich] and [filter].
 *
 * @since 1.0.0
 */
@DavilsDsl
public class LoggingBuilder internal constructor() : ValidatingBuilder<LoggingConfiguration>() {
    /**
     * Names the logging in a report.
     *
     * @since 1.0.0
     */
    override val subject: String get() = "logging"

    /**
     * The threshold of every logger without one of its own; [LogLevel.Info] by default.
     *
     * @since 1.0.0
     */
    public var level: LogLevel = LogLevel.Info

    /**
     * The clock of the timestamps; the clock of the system by default.
     *
     * @since 1.0.0
     */
    public var clock: Clock = Clock.System

    /**
     * The time source of durations, windows and rate limits; the monotonic one by default.
     *
     * @since 1.0.0
     */
    public var timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic

    /**
     * The environment read by the console sinks and the enrichment; the one of the process by default. Set it before
     * declaring sinks and enrichment, which take it when they are declared.
     *
     * @since 1.0.0
     */
    public var environment: Map<String, String> = processEnvironment()

    /**
     * Where the logging reports its own trouble; [LogDiagnostics.StandardError] by default.
     *
     * @since 1.0.0
     */
    public var diagnostics: LogDiagnostics = LogDiagnostics.StandardError

    /**
     * The rules of [Logging.health]; [HealthRules.DEFAULT] by default.
     *
     * @since 1.0.0
     */
    public var healthRules: List<HealthRule> = HealthRules.DEFAULT

    internal val loggers = LinkedHashMap<String, LoggerConfiguration>()
    internal val sinks = LinkedHashMap<String, LogSinkDefinition>()
    internal val filters = mutableListOf<LogFilter>()
    internal val listeners = mutableListOf<LoggingEventListener>()
    internal var intake: IntakeConfiguration? = null
    internal var masking: MaskingConfiguration? = null
    internal var sampling: SamplingConfiguration? = null
    internal var enrichment: Map<String, LogValue> = emptyMap()

    /**
     * Configures the logger [name] and the loggers below it; a logger declared twice is reported.
     *
     * @param name the dotted name of the logger; empty for the root.
     * @param block configures the logger.
     * @since 1.0.0
     */
    public fun logger(name: String, block: LoggerBuilder.() -> Unit) {
        collect(target = loggers, key = name, builder = LoggerBuilder(), property = ::loggers, block = block)
    }

    /**
     * Configures the intake.
     *
     * @param block configures the intake.
     * @since 1.0.0
     */
    public fun intake(block: IntakeBuilder.() -> Unit) {
        intake = nested(IntakeBuilder(), ::intake, block)
    }

    /**
     * Configures the masking; without it, the default masking of secrets applies.
     *
     * @param block configures the masking.
     * @since 1.0.0
     */
    public fun mask(block: MaskingBuilder.() -> Unit) {
        masking = nested(MaskingBuilder(), ::masking, block)
    }

    /**
     * Configures the sampling; without it, every record is written.
     *
     * @param block configures the sampling.
     * @since 1.0.0
     */
    public fun sample(block: SamplingBuilder.() -> Unit) {
        sampling = nested(SamplingBuilder(), ::sampling, block)
    }

    /**
     * Adds fields to the context of every record.
     *
     * @param block adds the fields.
     * @since 1.0.0
     */
    public fun enrich(block: EnrichmentBuilder.() -> Unit) {
        checkOpen()
        val builder = EnrichmentBuilder(environment)
        builder.block()
        enrichment = enrichment + builder.fields()
    }

    /**
     * Adds a filter that every record must pass.
     *
     * @param filter the filter.
     * @since 1.0.0
     */
    public fun filter(filter: LogFilter) {
        checkOpen()
        filters += filter
    }

    /**
     * Adds a listener of the events of the logging.
     *
     * @param listener the listener.
     * @since 1.0.0
     */
    public fun onEvent(listener: LoggingEventListener) {
        checkOpen()
        listeners += listener
    }

    /**
     * Declares a sink of a module that builds on kheck, such as `console { }` does.
     *
     * @param C the configuration of the sink.
     * @param B the builder of the sink.
     * @param name the name of the sink; a name declared twice is reported.
     * @param builder a fresh builder of the sink.
     * @param block configures the sink.
     * @param create creates the sink from its configuration, once per configuration that is applied.
     * @since 1.0.0
     */
    @DavilsInternal
    public fun <C : LogSinkConfiguration, B : LogSinkBuilder<C>> register(
        name: String,
        builder: B,
        block: B.() -> Unit,
        create: (C) -> LogSink,
    ) {
        val declared = LinkedHashMap<String, C>()
        collect(target = declared, key = name, builder = builder, property = ::sinks, block = block)
        val configuration = declared[name] ?: return
        sinks[name] = LogSinkDefinition(name, configuration.options) { create(configuration) }
    }

    /**
     * Creates the configuration.
     *
     * @return the configuration, validated afterwards.
     * @since 1.0.0
     */
    override fun assemble(): LoggingConfiguration = LoggingConfiguration(this)
}
