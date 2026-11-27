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

import com.davils.kheck.core.integrity.IntegrityChain
import com.davils.kheck.core.integrity.IntegrityOptions
import com.davils.kheck.core.integrity.LogSeal
import com.davils.kheck.core.sink.LogLine
import com.davils.kore.core.failure.attempted
import com.davils.krypt.primitive.Crypto
import com.davils.krypt.primitive.key.SecretKey

internal class SealChain(
    private val crypto: Crypto,
    private val options: IntegrityOptions,
    private val key: SecretKey,
) {
    private var committed: LogSeal = LogSeal.origin(options.algorithm)
    private var pending: LogSeal = committed

    suspend fun anchored(): Throwable? {
        val anchor = options.anchor ?: return null
        val result = attempted { anchor() }
        result.getOrNull()?.let { seal ->
            committed = seal
            pending = seal
        }
        return result.exceptionOrNull()
    }

    suspend fun sealed(lines: List<LogLine>): List<LogLine> {
        var previous = committed
        val sealed = lines.map { line ->
            val seal = IntegrityChain.sealed(
                crypto = crypto,
                key = key,
                algorithm = options.algorithm,
                previous = previous,
                text = line.text,
            )
            previous = seal
            LogLine(record = line.record, text = line.text, seal = seal)
        }
        pending = previous
        return sealed
    }

    fun committed() {
        committed = pending
    }
}
