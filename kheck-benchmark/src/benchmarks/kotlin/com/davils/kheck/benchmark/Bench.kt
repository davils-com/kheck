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

package com.davils.kheck.benchmark

import com.davils.kheck.core.Logging
import com.davils.kheck.core.LoggingBuilder
import com.davils.kheck.core.logging
import com.davils.krypt.core.SecretBytes
import com.davils.krypt.primitive.key.SecretKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

internal class Bench(block: LoggingBuilder.() -> Unit) {
    private val dispatcher = Executors.newFixedThreadPool(THREADS).asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    val logging: Logging = logging(scope, dispatcher, block)

    fun closed() {
        runBlocking { logging.close() }
        scope.cancel()
        dispatcher.close()
    }

    private companion object {
        private const val THREADS: Int = 2
    }
}

internal fun benchmarkKey(): SecretKey {
    val bytes = SecretBytes.copyOf("benchmark-key-0123456789".encodeToByteArray())
    return bytes.use(SecretKey::of)
}
