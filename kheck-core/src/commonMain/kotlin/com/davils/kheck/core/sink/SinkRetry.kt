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
 * How often a sink tries a batch that failed before it hands the batch to its fallback, waiting [backoff] before the
 * second attempt and twice as long before each further one, up to [maxBackoff].
 *
 * The waits suspend the coroutine of the sink only, so other sinks go on; the queue of the sink fills meanwhile.
 *
 * @property attempts the attempts in total, the first included; 1 tries once.
 * @property backoff the wait before the second attempt.
 * @property maxBackoff the longest wait between two attempts.
 * @since 1.0.0
 */
public class SinkRetry(public val attempts: Int, public val backoff: Duration, public val maxBackoff: Duration) {
    /**
     * Shows the retry.
     *
     * @return the retry as text.
     * @since 1.0.0
     */
    override fun toString(): String = "SinkRetry(attempts=$attempts, backoff=$backoff, maxBackoff=$maxBackoff)"

    /**
     * The retry that tries once.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * One attempt and no wait.
         *
         * @since 1.0.0
         */
        public val NONE: SinkRetry = SinkRetry(attempts = 1, backoff = Duration.ZERO, maxBackoff = Duration.ZERO)
    }
}
