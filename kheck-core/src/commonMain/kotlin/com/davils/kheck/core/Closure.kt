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

/**
 * How [Logging.close] with a timeout ended.
 *
 * @since 1.0.0
 */
public sealed interface Closure {
    /**
     * Every waiting record was written and every sink flushed and closed in time.
     *
     * @since 1.0.0
     */
    public object Completed : Closure {
        /**
         * Names the closure.
         *
         * @return `Completed`.
         * @since 1.0.0
         */
        override fun toString(): String = "Completed"
    }

    /**
     * The timeout ended the closing: the coroutines of the logging were cancelled and the records still waiting were
     * dropped and counted in `droppedAfterClose` of the statistics.
     *
     * @property pending the records that were still waiting in the intake and the queues of the sinks.
     * @since 1.0.0
     */
    public class TimedOut internal constructor(public val pending: Long) : Closure {
        /**
         * Compares by value.
         *
         * @param other the object to compare with.
         * @return whether [other] is an equal [TimedOut].
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is TimedOut && pending == other.pending

        /**
         * Returns a hash code consistent with [equals].
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = pending.hashCode()

        /**
         * Shows the pending records.
         *
         * @return the closure as text.
         * @since 1.0.0
         */
        override fun toString(): String = "TimedOut(pending=$pending)"
    }
}
