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

@file:OptIn(ExperimentalAtomicApi::class, DavilsInternal::class)

package com.davils.kheck.slf4j

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.Logger
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogEvent
import com.davils.kheck.core.Logging
import com.davils.kore.core.annotation.DavilsInternal
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.withLock

/**
 * Connects SLF4J to a kheck logging: every logger of SLF4J writes into the logging that is [attached].
 *
 * SLF4J finds its provider through the service loader and keeps it for the whole process, so this bridge is the one
 * piece of process-wide state of kheck. An application attaches its logging once it has started it; records logged
 * through SLF4J before that are kept, up to 1 024, and handed to the logging when it is attached, and the oldest of
 * them are dropped and counted in [lost] when there are more. While nothing is attached, the level `INFO` and above
 * counts as enabled. Safe to use from any thread.
 *
 * @since 1.0.0
 */
public object Slf4jBridge {
    private const val MAX_PENDING: Int = 1_024

    private val attached = AtomicReference<Logging?>(null)
    internal val lock: ReentrantLock = ReentrantLock()
    private val pending = ArrayDeque<PendingEvent>()
    private val lostCount = AtomicLong(0L)

    /**
     * How many records logged before a logging was attached were dropped because more than 1 024 waited.
     *
     * @since 1.0.0
     */
    public val lost: Long get() = lostCount.load()

    /**
     * Whether a logging is attached.
     *
     * @since 1.0.0
     */
    public val isAttached: Boolean get() = attached.load() != null

    /**
     * Writes every logger of SLF4J into [logging] and hands it the records that waited.
     *
     * @param logging the logging; replaces one attached before.
     * @since 1.0.0
     */
    public fun attach(logging: Logging) {
        lock.withLock {
            attached.store(logging)
            pending.forEach { event -> logging.bridged(event.logger).emit(event.level, event.event) }
            pending.clear()
        }
    }

    /**
     * Detaches the logging, such as before it is closed; later records wait for the next one.
     *
     * @since 1.0.0
     */
    public fun detach() {
        attached.store(null)
    }

    internal fun isEnabled(logger: String, level: LogLevel): Boolean {
        val logging = attached.load() ?: return level.isAtLeast(LogLevel.Info)
        return logging.bridged(logger).isEnabled(level)
    }

    internal fun emit(logger: String, level: LogLevel, event: LogEvent) {
        val logging = attached.load() ?: attachedOrPended(PendingEvent(logger, level, event)) ?: return
        logging.bridged(logger).emit(level, event)
    }

    private fun attachedOrPended(event: PendingEvent): Logging? = lock.withLock {
        val logging = attached.load()
        if (logging == null) pended(event)
        logging
    }

    private fun pended(event: PendingEvent) {
        if (pending.size >= MAX_PENDING) {
            pending.removeFirst()
            lostCount.addAndFetch(1L)
        }
        pending.addLast(event)
    }
}

private fun Logging.bridged(name: String): Logger = logger(LoggerName.bounded(name).value)
