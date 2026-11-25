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

package com.davils.kheck.core.engine

import com.davils.kheck.core.LoggingConfiguration
import com.davils.kheck.core.diagnostics.typeOf
import com.davils.kheck.core.engine.route.Router
import com.davils.kheck.core.engine.worker.SinkWorker
import com.davils.kheck.core.engine.worker.WorkerContext
import com.davils.kheck.core.mask.engine.Masker
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.record.LogSource
import com.davils.kheck.core.sample.engine.Sampler
import com.davils.kheck.core.sample.engine.SamplingDecision
import com.davils.kheck.core.statistics.Suppression
import com.davils.kore.core.failure.contained
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class Pipeline(private val configuration: LoggingConfiguration, private val context: PipelineContext) {
    private val telemetry = context.telemetry
    private val sampler = Sampler(configuration.sampling, configuration.timeSource)
    private val masker = Masker(configuration.masking, telemetry::masked)
    private val router = Router(configuration)
    private val workerContext = WorkerContext(
        scope = context.scope,
        dispatcher = context.dispatcher,
        telemetry = telemetry,
        diagnostics = configuration.diagnostics,
        timeSource = configuration.timeSource,
        notices = context.notices,
    )
    private val fallbacks = HashMap<String, SinkWorker>()
    private val workers: Map<String, SinkWorker> = configuration.sinks.mapValues { (_, definition) ->
        SinkWorker(definition, workerContext) { name -> fallbacks.getValue(name) }
    }
    private var ticker: Job? = null

    init {
        fallbacks.putAll(workers)
        telemetry.retainSinks(workers.keys)
    }

    fun start() {
        workers.values.forEach(SinkWorker::start)
        val window = configuration.sampling.deduplication?.window ?: return
        ticker = context.scope.launch(context.dispatcher) {
            while (true) {
                delay(window)
                context.ticks()
            }
        }
    }

    suspend fun handle(pending: PendingRecord) {
        val record = assembled(pending)
        val decision = sampler.decide(record)
        val suppression = SUPPRESSIONS[decision]
        if (suppression == null) deliver(record)
        if (suppression != null) telemetry.suppressed(suppression)
        expired()
    }

    suspend fun tick() {
        expired()
    }

    suspend fun flush() {
        workers.values.forEach { worker -> worker.flush() }
    }

    suspend fun close() {
        ticker?.cancel()
        expired()
        workers.values.forEach { worker -> worker.close() }
    }

    private suspend fun expired() {
        sampler.expired().forEach { record -> deliver(record) }
    }

    private suspend fun deliver(record: LogRecord) {
        if (!isAccepted(record)) {
            telemetry.suppressed(Suppression.Filtered)
            return
        }
        telemetry.processed(record.level)
        val masked = masked(record) ?: return
        router.sinks(record.source.logger).forEach { name -> workers.getValue(name).offer(masked) }
    }

    private fun masked(record: LogRecord): LogRecord? {
        val result = contained { masker.record(record) }
        result.onFailure { failure ->
            val type = typeOf(failure)
            configuration.diagnostics.report("a record was dropped because masking failed: $type")
        }
        return result.getOrNull()
    }

    private fun isAccepted(record: LogRecord): Boolean = configuration.filters.all { filter ->
        val result = contained { filter.accept(record) }
        result.onFailure { failure ->
            configuration.diagnostics.report("a filter failed: ${typeOf(failure)}")
        }
        result.getOrDefault(true)
    }

    private fun assembled(pending: PendingRecord): LogRecord = LogRecord(
        sequence = pending.sequence,
        timestamp = pending.timestamp,
        level = pending.level,
        source = LogSource(logger = pending.logger, thread = pending.thread, caller = pending.caller),
        message = pending.event.message,
        fields = pending.event.fields,
        context = configuration.enrichment + pending.context,
        markers = pending.event.markers,
        cause = pending.event.cause?.let { throwable -> CauseCapture.capture(throwable) },
        omitted = pending.event.omitted,
    )

    private companion object {
        private val SUPPRESSIONS: Map<SamplingDecision, Suppression> = mapOf(
            SamplingDecision.Sampled to Suppression.Sampled,
            SamplingDecision.RateLimited to Suppression.RateLimited,
            SamplingDecision.Deduplicated to Suppression.Deduplicated,
        )
    }
}
