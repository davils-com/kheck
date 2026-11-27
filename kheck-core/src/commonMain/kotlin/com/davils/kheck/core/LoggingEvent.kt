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

import com.davils.kheck.core.diagnostics.typeOf
import com.davils.konfetti.core.Event
import com.davils.konfetti.core.TopicKey
import com.davils.kore.core.validation.report.ValidationReport

/**
 * What a logging announces to its [listeners][LoggingEventListener]: trouble of sinks, lost records and changes of
 * its configuration.
 *
 * @since 1.0.0
 */
public sealed interface LoggingEvent : Event {
    /**
     * A sink failed after it had worked, or at its first attempt.
     *
     * @property sink the name of the sink.
     * @property cause what the sink threw; its message is foreign text.
     * @since 1.0.0
     */
    public class SinkFailed internal constructor(public val sink: String, public val cause: Throwable) : LoggingEvent {
        /**
         * Shows the sink and the type of the cause, not its message.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "SinkFailed(sink=$sink, cause=${typeOf(cause)})"
    }

    /**
     * A sink that failed wrote again.
     *
     * @property sink the name of the sink.
     * @since 1.0.0
     */
    public class SinkRecovered internal constructor(public val sink: String) : LoggingEvent {
        /**
         * Shows the sink.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "SinkRecovered(sink=$sink)"
    }

    /**
     * A stage of a sink, such as an open circuit breaker, refused a batch, which went to the fallback of the sink.
     *
     * @property sink the name of the sink.
     * @property count the lines of the refused batch.
     * @since 1.0.0
     */
    public class SinkRejected internal constructor(public val sink: String, public val count: Int) : LoggingEvent {
        /**
         * Shows the sink and the count.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "SinkRejected(sink=$sink, count=$count)"
    }

    /**
     * Records were lost because the queue of a sink was full; announced with the next batch of the sink.
     *
     * @property sink the name of the sink.
     * @property count the records lost since the last announcement.
     * @since 1.0.0
     */
    public class RecordsDropped internal constructor(public val sink: String, public val count: Long) : LoggingEvent {
        /**
         * Shows the sink and the count.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "RecordsDropped(sink=$sink, count=$count)"
    }

    /**
     * A new configuration was applied.
     *
     * @since 1.0.0
     */
    public object ConfigurationApplied : LoggingEvent {
        /**
         * Shows the name of the event.
         *
         * @return `ConfigurationApplied`.
         * @since 1.0.0
         */
        override fun toString(): String = "ConfigurationApplied"
    }

    /**
     * A new configuration was refused; the logging keeps the one it had.
     *
     * @property report why the configuration was refused.
     * @since 1.0.0
     */
    public class ConfigurationRejected internal constructor(public val report: ValidationReport) : LoggingEvent {
        /**
         * Shows the count of errors.
         *
         * @return the event as text.
         * @since 1.0.0
         */
        override fun toString(): String = "ConfigurationRejected(errors=${report.errors.size})"
    }

    /**
     * The topic of a konfetti bus that `events(bus)` publishes the events to.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The key of the topic, `kheck.logging`.
         *
         * @since 1.0.0
         */
        public val TOPIC: TopicKey<LoggingEvent> = TopicKey("kheck.logging")
    }
}
