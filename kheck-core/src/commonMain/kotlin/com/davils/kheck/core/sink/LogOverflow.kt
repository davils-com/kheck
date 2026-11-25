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

package com.davils.kheck.core.sink

import kotlin.time.Duration

/**
 * What happens to a record when a queue is full; every record lost is counted in the statistics.
 *
 * @since 1.0.0
 */
public sealed interface LogOverflow {
    /**
     * Drops the new record.
     *
     * @since 1.0.0
     */
    public object DropNewest : LogOverflow {
        /**
         * Shows the name of the overflow.
         *
         * @return `DropNewest`.
         * @since 1.0.0
         */
        override fun toString(): String = "DropNewest"
    }

    /**
     * Drops the oldest waiting record to make room for the new one.
     *
     * @since 1.0.0
     */
    public object DropOldest : LogOverflow {
        /**
         * Shows the name of the overflow.
         *
         * @return `DropOldest`.
         * @since 1.0.0
         */
        override fun toString(): String = "DropOldest"
    }

    /**
     * Makes the caller wait up to [maxWait] for room, and drops the new record after that.
     *
     * The intake blocks the thread of the caller, a sink suspends the coroutine of the logging. On WebAssembly there is
     * a single thread that cannot wait, so the intake drops the new record at once there.
     *
     * @property maxWait the longest wait.
     * @since 1.0.0
     */
    public class Block(public val maxWait: Duration) : LogOverflow {
        /**
         * Returns whether [other] waits as long.
         *
         * @param other the object to compare with.
         * @return `true` if both overflows are equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Block && other.maxWait == maxWait

        /**
         * Returns the hash code of the wait.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = maxWait.hashCode()

        /**
         * Shows the wait.
         *
         * @return the overflow as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Block(maxWait=$maxWait)"
    }
}
