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

package com.davils.kheck.core.engine

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.Logger
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogEvent
import com.davils.kheck.core.Closure
import com.davils.kheck.core.Limits
import com.davils.kheck.core.LogLevels
import com.davils.kheck.core.LoggingConfiguration
import com.davils.kheck.core.LoggingEvent
import com.davils.kheck.core.Reconfiguration
import com.davils.kheck.core.engine.route.LevelResolver
import com.davils.kheck.core.engine.telemetry.LoggingTelemetry
import com.davils.kheck.core.internal.platform.boundContextFields
import com.davils.kheck.core.internal.platform.currentThreadName
import com.davils.kheck.core.internal.platform.isInsideSink
import com.davils.kheck.core.internal.platform.locateCaller
import com.davils.kheck.core.record.LogFrame
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

internal class LoggingEngine(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    initial: LoggingConfiguration,
    private val maxLoggers: Int = Limits.MAX_LOGGERS,
) {
    private val current = AtomicReference(initial)
    private val sequence = AtomicLong(0L)
    private val closed = AtomicBoolean(false)
    private val loggers = AtomicReference<Map<LoggerName, EngineLogger>>(emptyMap())
    private val generations = AtomicLong(0L)

    val telemetry: LoggingTelemetry = LoggingTelemetry(
        intakeCapacity = initial.intake.capacity,
        listeners = { current.load().listeners },
        diagnostics = { current.load().diagnostics },
        events = { current.load().events },
    )

    private val intake = Intake(initial.intake, telemetry)
    private val notices = Notices(clock = { current.load().clock }, sequence = ::nextSequence)
    private val context = PipelineContext(
        scope = scope,
        dispatcher = dispatcher,
        telemetry = telemetry,
        notices = notices,
        ticks = { intake.command(IntakeControl.Tick) },
    )

    val levels: LogLevels = LogLevels(::refreshed)

    private val processor = Processor(
        intake = intake,
        pipelines = { configuration -> Pipeline(configuration, context) },
        initial = initial,
        diagnostics = { current.load().diagnostics },
    )

    private val job: Job = scope.launch(dispatcher) { processor.run() }

    val configuration: LoggingConfiguration get() = current.load()

    val isClosed: Boolean get() = closed.load()

    val generation: Long get() = generations.load()

    fun logger(name: LoggerName): Logger {
        val known = loggers.load()[name]
        if (known != null) return known
        return remembered(name, EngineLogger(name, this))
    }

    fun resolver(): LevelResolver = LevelResolver(current.load(), levels.overrides.value)

    fun submit(logger: EngineLogger, level: LogLevel, event: LogEvent) {
        if (closed.load()) {
            telemetry.closedDropped()
            return
        }
        if (isInsideSink()) {
            telemetry.reentrant()
            return
        }
        intake.offer(
            PendingRecord(
                sequence = nextSequence(),
                timestamp = current.load().clock.now(),
                level = level,
                logger = logger.name,
                thread = currentThreadName(),
                event = event,
                context = boundContextFields(),
                caller = callerOf(logger),
            ),
        )
    }

    suspend fun flush() {
        if (closed.load()) return
        awaited(IntakeControl.Flush())
    }

    suspend fun reconfigure(configuration: LoggingConfiguration): Reconfiguration {
        if (closed.load()) return Reconfiguration.Closed
        current.store(configuration)
        refreshed()
        awaited(IntakeControl.Swap(configuration))
        telemetry.publish(LoggingEvent.ConfigurationApplied)
        return Reconfiguration.Applied
    }

    suspend fun close() {
        closing()
        job.join()
    }

    suspend fun close(timeout: Duration): Closure {
        closing()
        val isJoined = withTimeoutOrNull(timeout) { job.join() } != null
        if (isJoined) return Closure.Completed
        val statistics = telemetry.statistics()
        val pending = statistics.intakeQueued + statistics.sinks.values.sumOf { sink -> sink.queued }
        job.cancelAndJoin()
        telemetry.closedDropped(pending)
        return Closure.TimedOut(pending)
    }

    suspend fun flush(timeout: Duration): Boolean {
        if (closed.load()) return true
        return withTimeoutOrNull(timeout) { awaited(IntakeControl.Flush()) } != null
    }

    private fun closing() {
        if (closed.compareAndSet(expectedValue = false, newValue = true)) intake.command(IntakeControl.Close())
    }

    private fun remembered(name: LoggerName, created: EngineLogger): EngineLogger {
        while (true) {
            val known = loggers.load()
            val kept = known[name] ?: created
            val isStored = name in known || known.size >= maxLoggers
            if (isStored || loggers.compareAndSet(expectedValue = known, newValue = known + (name to created))) {
                return kept
            }
        }
    }

    private suspend fun awaited(command: IntakeControl.Completable) {
        intake.command(command)
        select {
            command.done.onAwait { }
            job.onJoin { }
        }
    }

    private fun refreshed() {
        generations.addAndFetch(1L)
    }

    private fun callerOf(logger: EngineLogger): LogFrame? {
        if (!logger.isCallerIncluded) return null
        return locateCaller(SKIPPED_CALLERS)
    }

    private fun nextSequence(): Long = sequence.addAndFetch(1L)

    private companion object {
        private val SKIPPED_CALLERS: List<String> = listOf(
            "com.davils.kheck.core.engine.",
            "com.davils.kheck.core.internal.",
            "com.davils.kheck.slf4j.",
            "com.davils.kheck.jul.",
            "org.slf4j.",
            "java.util.logging.",
            "java.lang.Thread",
            "java.lang.StackWalker",
        )
    }
}
