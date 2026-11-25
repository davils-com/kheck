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

package com.davils.kheck.core.engine

import com.davils.kheck.core.engine.mailbox.Mail
import com.davils.kheck.core.engine.mailbox.Mailbox
import com.davils.kheck.core.engine.mailbox.boundedChannel
import com.davils.kheck.core.engine.telemetry.LoggingTelemetry
import com.davils.kheck.core.intake.IntakeConfiguration
import com.davils.kheck.core.internal.platform.sendBlocking
import com.davils.kheck.core.sink.LogOverflow
import kotlinx.coroutines.channels.Channel

internal class Intake(private val configuration: IntakeConfiguration, private val telemetry: LoggingTelemetry) {
    private val records: Channel<PendingRecord> =
        boundedChannel(configuration.capacity, configuration.overflow) { telemetry.displaced() }
    private val control: Channel<IntakeControl> = Channel(Channel.UNLIMITED)
    private val mailbox = Mailbox(records, control)

    fun offer(record: PendingRecord) {
        if (records.trySend(record).isSuccess) {
            telemetry.accepted()
            return
        }
        val overflow = configuration.overflow
        val isDelivered = overflow is LogOverflow.Block && sendBlocking(records, record, overflow.maxWait)
        if (isDelivered) {
            telemetry.accepted()
            return
        }
        telemetry.intakeDropped()
    }

    fun command(command: IntakeControl) {
        control.trySend(command)
    }

    suspend fun next(): Mail<PendingRecord, IntakeControl> {
        val mail = mailbox.next()
        if (mail is Mail.Record) telemetry.taken()
        return mail
    }

    fun abandoned() {
        while (mailbox.nextRecord() != null) {
            telemetry.taken()
            telemetry.closedDropped()
        }
    }
}
