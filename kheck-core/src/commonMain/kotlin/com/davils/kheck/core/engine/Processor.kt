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

import com.davils.kheck.core.LoggingConfiguration
import com.davils.kheck.core.diagnostics.LogDiagnostics
import com.davils.kheck.core.diagnostics.typeOf
import com.davils.kheck.core.engine.mailbox.Mail
import com.davils.kore.core.failure.attempted

internal class Processor(
    private val intake: Intake,
    private val pipelines: (LoggingConfiguration) -> Pipeline,
    initial: LoggingConfiguration,
    private val diagnostics: () -> LogDiagnostics,
) {
    private var pipeline: Pipeline = pipelines(initial)

    suspend fun run() {
        pipeline.start()
        while (true) {
            val isClosed = when (val mail = intake.next()) {
                is Mail.Record -> handled(mail.value)
                is Mail.Control -> controlled(mail.value)
            }
            if (isClosed) return
        }
    }

    private suspend fun handled(pending: PendingRecord): Boolean {
        val result = attempted { pipeline.handle(pending) }
        result.onFailure { failure ->
            diagnostics().report("a record could not be processed: ${typeOf(failure)}")
        }
        return false
    }

    private suspend fun controlled(command: IntakeControl): Boolean {
        when (command) {
            is IntakeControl.Flush -> pipeline.flush()
            is IntakeControl.Swap -> swapped(command.configuration)
            is IntakeControl.Close -> closed()
            IntakeControl.Tick -> pipeline.tick()
        }
        if (command is IntakeControl.Completable) command.done.complete(Unit)
        return command is IntakeControl.Close
    }

    private suspend fun swapped(configuration: LoggingConfiguration) {
        pipeline.close()
        pipeline = pipelines(configuration)
        pipeline.start()
    }

    private suspend fun closed() {
        pipeline.close()
        intake.abandoned()
    }
}
