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

package com.davils.kheck.core.engine.worker

import com.davils.kheck.core.LoggingEvent
import com.davils.kheck.core.diagnostics.typeOf
import com.davils.kheck.core.engine.mailbox.Mail
import com.davils.kheck.core.engine.mailbox.Mailbox
import com.davils.kheck.core.engine.mailbox.boundedChannel
import com.davils.kheck.core.filter.LogFilter
import com.davils.kheck.core.format.LogEncoder
import com.davils.kheck.core.internal.platform.sinkBinding
import com.davils.kheck.core.record.LogRecord
import com.davils.kheck.core.sink.LogLine
import com.davils.kheck.core.sink.LogOverflow
import com.davils.kheck.core.sink.LogSink
import com.davils.kheck.core.sink.LogSinkDefinition
import com.davils.kore.core.failure.attempted
import com.davils.kore.core.failure.contained
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal class SinkWorker(
    private val definition: LogSinkDefinition,
    private val context: WorkerContext,
    private val fallbacks: (String) -> SinkWorker,
) {
    private val options = definition.options
    private val tally = context.telemetry.sink(definition.name)
    private val records: Channel<Delivery> = boundedChannel(options.capacity, options.overflow) { tally.displaced() }
    private val control: Channel<WorkerControl> = Channel(Channel.UNLIMITED)
    private val mailbox = Mailbox(records, control)
    private val sink: LogSink = definition.create()
    private val buffer = StringBuilder()
    private val encoder: LogEncoder by lazy { options.format.encoder(sink.colorDepth(options.format.colorMode)) }
    private var reported: Long = 0L
    private val job: Job = context.scope.launch(context.dispatcher + sinkBinding(), CoroutineStart.LAZY) { run() }

    fun start() {
        job.start()
    }

    suspend fun offer(record: LogRecord) {
        if (!accepts(record)) return
        deliver(Delivery(record, isForwarded = false))
    }

    suspend fun forward(record: LogRecord) {
        deliver(Delivery(record, isForwarded = true))
    }

    suspend fun flush() {
        commanded(WorkerControl.Flush())
    }

    suspend fun close() {
        commanded(WorkerControl.Close())
        job.join()
    }

    private fun accepts(record: LogRecord): Boolean {
        if (!record.level.isAtLeast(options.level)) return false
        val isMarked = options.markers.isEmpty() || record.markers.any { marker -> marker in options.markers }
        return isMarked && options.filters.all { filter -> filter.isAccepting(record) }
    }

    private suspend fun commanded(command: WorkerControl) {
        control.send(command)
        select {
            command.done.onAwait { }
            job.onJoin { }
        }
    }

    private suspend fun deliver(delivery: Delivery) {
        if (records.trySend(delivery).isSuccess) {
            tally.enqueued()
            return
        }
        val overflow = options.overflow
        val isDelivered = overflow is LogOverflow.Block && blocked(delivery, overflow.maxWait)
        if (isDelivered) return
        tally.dropped()
    }

    private suspend fun blocked(delivery: Delivery, maxWait: Duration): Boolean {
        val isDelivered = withTimeoutOrNull(maxWait) { records.send(delivery) } != null
        if (isDelivered) tally.enqueued()
        return isDelivered
    }

    private suspend fun run() {
        opened()
        while (true) {
            val isClosed = when (val mail = mailbox.next()) {
                is Mail.Record -> batch(mail.value)
                is Mail.Control -> handled(mail.value)
            }
            if (isClosed) return
        }
    }

    private suspend fun opened() {
        val failure = attempted { sink.open() }.exceptionOrNull() ?: return
        failed(failure, emptyList())
    }

    private suspend fun handled(command: WorkerControl): Boolean {
        val flushed = attempted { sink.flush() }
        flushed.exceptionOrNull()?.let { failure -> failed(failure, emptyList()) }
        if (command is WorkerControl.Close) {
            val closed = attempted { sink.close() }
            closed.exceptionOrNull()?.let { failure -> failed(failure, emptyList()) }
        }
        command.done.complete(Unit)
        return command is WorkerControl.Close
    }

    private suspend fun batch(first: Delivery): Boolean {
        val deliveries = mutableListOf(first)
        while (deliveries.size < options.maxBatch) {
            deliveries += mailbox.nextRecord() ?: break
        }
        tally.dequeued(deliveries.size)
        val lines = notice() + deliveries.mapNotNull { delivery -> line(delivery.record) }
        if (lines.isNotEmpty()) write(lines, deliveries)
        return false
    }

    private suspend fun write(lines: List<LogLine>, deliveries: List<Delivery>) {
        val started = context.timeSource.markNow()
        val failure = attempts(lines)
        if (failure != null) {
            tally.failed(lines.size)
            failed(failure, deliveries)
            return
        }
        tally.written(lines.size, started.elapsedNow())
        recovered()
    }

    private suspend fun attempts(lines: List<LogLine>): Throwable? {
        val retry = options.retry
        var wait = retry.backoff
        var failure: Throwable? = null
        for (attempt in 1..retry.attempts) {
            if (attempt > 1) {
                delay(wait)
                wait = (wait * 2).coerceAtMost(retry.maxBackoff)
            }
            failure = attempted { sink.write(lines) }.exceptionOrNull() ?: return null
        }
        return failure
    }

    private fun notice(): List<LogLine> {
        val lost = tally.droppedCount - reported
        if (lost <= 0L) return emptyList()
        reported += lost
        val record = context.notices.dropped(definition.name, lost)
        context.telemetry.publish(LoggingEvent.RecordsDropped(definition.name, lost))
        return listOfNotNull(line(record))
    }

    private fun line(record: LogRecord): LogLine? {
        buffer.setLength(0)
        val result = contained { encoder.encode(record, buffer) }
        result.exceptionOrNull()?.let { failure ->
            val type = typeOf(failure)
            context.diagnostics.report("sink '${definition.name}' could not encode a record: $type")
        }
        if (result.isFailure) return null
        return LogLine(record, buffer.toString())
    }

    private suspend fun failed(failure: Throwable, deliveries: List<Delivery>) {
        if (tally.failing(true)) {
            context.diagnostics.report("sink '${definition.name}' failed: ${typeOf(failure)}")
            context.telemetry.publish(LoggingEvent.SinkFailed(definition.name, failure))
        }
        val name = options.fallback ?: return
        val target = fallbacks(name)
        deliveries.filterNot(Delivery::isForwarded).forEach { delivery -> target.forward(delivery.record) }
    }

    private fun recovered() {
        if (!tally.failing(false)) return
        context.diagnostics.report("sink '${definition.name}' recovered")
        context.telemetry.publish(LoggingEvent.SinkRecovered(definition.name))
    }

    private fun LogFilter.isAccepting(record: LogRecord): Boolean {
        val result = contained { accept(record) }
        result.onFailure { failure ->
            context.diagnostics.report("a filter of sink '${definition.name}' failed: ${typeOf(failure)}")
        }
        return result.getOrDefault(true)
    }
}
