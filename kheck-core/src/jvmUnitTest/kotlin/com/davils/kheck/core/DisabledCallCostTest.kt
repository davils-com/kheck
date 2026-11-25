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

import com.davils.kheck.api.debug
import com.davils.kheck.core.sink.custom.sink
import com.davils.kheck.testing.CapturingSink
import com.sun.management.ThreadMXBean
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.longs.shouldBeLessThan
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors

private const val WARM_UP: Int = 200_000
private const val CALLS: Int = 1_000_000
private const val ALLOWED_BYTES: Long = 512L * 1024L

class DisabledCallCostTest : FunSpec({
    context("a call below the level of its logger") {
        test("allocates nothing and reaches no sink, however often it runs") {
            val pool = Executors.newSingleThreadExecutor()
            val (allocated, messages) = pool.asCoroutineDispatcher().use { dispatcher -> measuredOn(dispatcher) }

            allocated shouldBeLessThan ALLOWED_BYTES
            messages.shouldBeEmpty()
        }
    }
})

private fun measuredOn(dispatcher: CoroutineDispatcher): Pair<Long, List<String>> = runBlocking {
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    val sink = CapturingSink()
    val logging = logging(scope, dispatcher) { sink("capture", sink) }
    val logger = logging.logger("kide.cost")
    val child = logger.child("pool")
    repeat(WARM_UP) { index -> child.debug { "warm $index" } }
    val before = allocatedBytes()
    repeat(CALLS) { index ->
        child.debug {
            field("index", index)
            "disabled $index"
        }
    }
    val allocated = allocatedBytes() - before
    logging.close()
    scope.cancel()
    allocated to sink.messages
}

private fun allocatedBytes(): Long {
    val threads = ManagementFactory.getThreadMXBean() as ThreadMXBean
    val current = Thread.currentThread()
    return threads.getThreadAllocatedBytes(current.threadId())
}
