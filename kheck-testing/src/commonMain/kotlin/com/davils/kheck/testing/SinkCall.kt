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

package com.davils.kheck.testing

/**
 * One call the logging made on a [CapturingSink].
 *
 * @since 1.0.0
 */
public sealed interface SinkCall {
    /**
     * The sink was opened.
     *
     * @since 1.0.0
     */
    public object Open : SinkCall {
        /**
         * Shows the name of the call.
         *
         * @return `Open`.
         * @since 1.0.0
         */
        override fun toString(): String = "Open"
    }

    /**
     * A batch was written.
     *
     * @property lines the size of the batch.
     * @since 1.0.0
     */
    public class Write(public val lines: Int) : SinkCall {
        /**
         * Returns whether [other] wrote a batch of the same size.
         *
         * @param other the object to compare with.
         * @return `true` if both are equal.
         * @since 1.0.0
         */
        override fun equals(other: Any?): Boolean = other is Write && other.lines == lines

        /**
         * Returns the size of the batch.
         *
         * @return the hash code.
         * @since 1.0.0
         */
        override fun hashCode(): Int = lines

        /**
         * Shows the size of the batch.
         *
         * @return the call as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Write($lines)"
    }

    /**
     * The sink was flushed.
     *
     * @since 1.0.0
     */
    public object Flush : SinkCall {
        /**
         * Shows the name of the call.
         *
         * @return `Flush`.
         * @since 1.0.0
         */
        override fun toString(): String = "Flush"
    }

    /**
     * The sink was closed.
     *
     * @since 1.0.0
     */
    public object Close : SinkCall {
        /**
         * Shows the name of the call.
         *
         * @return `Close`.
         * @since 1.0.0
         */
        override fun toString(): String = "Close"
    }
}
