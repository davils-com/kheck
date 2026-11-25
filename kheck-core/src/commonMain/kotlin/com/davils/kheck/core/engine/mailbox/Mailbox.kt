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

package com.davils.kheck.core.engine.mailbox

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select

internal class Mailbox<R : Any, C : Any>(private val records: Channel<R>, private val control: Channel<C>) {
    suspend fun next(): Mail<R, C> = ready() ?: awaited()

    fun nextRecord(): R? = records.tryReceive().getOrNull()

    private fun ready(): Mail<R, C>? {
        val record = records.tryReceive().getOrNull()
        if (record != null) return Mail.Record(record)
        val command = control.tryReceive().getOrNull()
        return command?.let { value -> Mail.Control(value) }
    }

    private suspend fun awaited(): Mail<R, C> = select {
        records.onReceive { value -> Mail.Record(value) }
        control.onReceive { value -> Mail.Control(value) }
    }
}
