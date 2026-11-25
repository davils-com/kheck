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

package com.davils.kheck.core.intake

import com.davils.kheck.core.sink.LogOverflow
import com.davils.kheck.core.sink.checkCapacity
import com.davils.kheck.core.sink.checkOverflow
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope

/**
 * The intake of a logging: the bounded queue between the loggers and the coroutine of the logging.
 *
 * A call of a logger never waits for the coroutine: it puts the record into the intake and returns, unless the
 * intake is full and [overflow] blocks. The intake cannot change while the logging runs.
 *
 * @property capacity the most records waiting.
 * @property overflow what happens to a record when the intake is full.
 * @since 1.0.0
 */
public class IntakeConfiguration(public val capacity: Int, public val overflow: LogOverflow) : Validatable {
    /**
     * Checks the capacity and the overflow.
     *
     * @since 1.0.0
     */
    override fun ValidationScope.validate() {
        checkCapacity(::capacity, capacity)
        checkOverflow(::overflow, overflow)
    }

    /**
     * Returns whether [other] has the same capacity and overflow.
     *
     * @param other the object to compare with.
     * @return `true` if both intakes are equal.
     * @since 1.0.0
     */
    override fun equals(other: Any?): Boolean =
        other is IntakeConfiguration && other.capacity == capacity && other.overflow == overflow

    /**
     * Returns a hash code consistent with [equals].
     *
     * @return the hash code.
     * @since 1.0.0
     */
    override fun hashCode(): Int = HASH_FACTOR * capacity + overflow.hashCode()

    /**
     * Shows the capacity and the overflow.
     *
     * @return the intake as text.
     * @since 1.0.0
     */
    override fun toString(): String = "IntakeConfiguration(capacity=$capacity, overflow=$overflow)"

    private companion object {
        private const val HASH_FACTOR: Int = 31
    }
}
