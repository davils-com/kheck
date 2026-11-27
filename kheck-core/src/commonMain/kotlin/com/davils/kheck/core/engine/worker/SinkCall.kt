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

import com.davils.kontinuity.circuitbreaker.CircuitBreaker
import com.davils.kontinuity.circuitbreaker.CircuitState
import com.davils.kontinuity.core.Instance
import com.davils.kontinuity.core.Stage
import com.davils.kontinuity.policy.Policy
import com.davils.kontinuity.timelimiter.TimeLimiter

internal class SinkCall(private val stages: List<Stage>) {
    private val flattened: List<Instance> = stages.flatMap { stage -> (stage as? Policy)?.stages ?: listOf(stage) }
    private val limiters: List<Stage> = flattened.filterIsInstance<TimeLimiter>()
    private val breaker: CircuitBreaker? = flattened.filterIsInstance<CircuitBreaker>().firstOrNull()

    val circuit: CircuitState? get() = breaker?.state

    suspend fun write(block: suspend () -> Unit) {
        through(stages, block)
    }

    suspend fun bounded(block: suspend () -> Unit) {
        through(limiters, block)
    }

    private suspend fun through(chain: List<Stage>, block: suspend () -> Unit) {
        val folded = chain.foldRight(block) { stage, inner -> { stage.execute(inner) } }
        folded()
    }
}
