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

import com.davils.kheck.api.info
import com.davils.kheck.core.format.plain.plain
import com.davils.kheck.core.sink.LogOverflow
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

private const val THREADS: Int = 8
private const val RECORDS: Int = 1_000
private const val QUEUE: Int = 32
private const val WORKERS: Int = 4

class LoggingStressTest : FunSpec({
    context("concurrency") {
        test("keeps every record and the order of each thread when every queue blocks") {
            val pool = Executors.newFixedThreadPool(WORKERS)
            pool.asCoroutineDispatcher().use { dispatcher ->
                stressed(dispatcher)
            }
        }
    }
})

private fun stressed(dispatcher: CoroutineDispatcher) {
    runBlocking {
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val sink = CapturingSink()
        val logging = logging(scope, dispatcher) {
            intake {
                capacity = QUEUE
                overflow = LogOverflow.Block(maxWait = 30.seconds)
            }
            sink("capture", sink) {
                capacity = QUEUE
                maxBatch = QUEUE
                overflow = LogOverflow.Block(maxWait = 30.seconds)
                format = plain { layout { message() } }
            }
        }
        val logger = logging.logger("stress")

        val writers = List(THREADS) { writer ->
            thread { repeat(RECORDS) { index -> logger.info { "$writer:$index" } } }
        }
        writers.forEach(Thread::join)
        logging.close()
        scope.cancel()

        val byWriter = sink.messages.groupBy { message -> message.substringBefore(':') }
        sink.messages.size shouldBe THREADS * RECORDS
        byWriter.values.forEach { messages ->
            val indices = messages.map { message -> message.substringAfter(':') }
            indices.map(String::toInt) shouldBe List(RECORDS) { index -> index }
        }
    }
}
