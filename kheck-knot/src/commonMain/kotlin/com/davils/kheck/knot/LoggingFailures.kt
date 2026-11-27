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

package com.davils.kheck.knot

import com.davils.kheck.api.error
import com.davils.kheck.core.Logging
import com.davils.knot.failure.sink.FailureSink

private const val DEFAULT_LOGGER: String = "failures"
private const val MESSAGE: String = "Unexpected failure"

/**
 * Returns a sink of knot-failure that writes every unexpected failure as an error record of the logger [logger], with
 * the failure as its cause and its id, fingerprint, severity, origin, thread and sink of knot as fields.
 *
 * The rendered text of the report is not logged: kheck writes the cause itself and escapes it where it is written.
 * Writing never blocks for long, as every call of a logger.
 *
 * @receiver the logging that writes the failures.
 * @param logger the name of the logger; `failures` by default.
 * @return the sink, for `sinks { sink(name, logging.failureSink()) }` of a failure handler of knot.
 * @since 1.0.0
 */
public fun Logging.failureSink(logger: String = DEFAULT_LOGGER): FailureSink {
    val target = logger(logger)
    return FailureSink { report ->
        val failure = report.failure
        target.error {
            cause(failure.cause)
            field(name = "failure.id", value = failure.id.value)
            field(name = "failure.fingerprint", value = failure.fingerprint.value)
            field(name = "failure.severity", value = failure.severity.name.lowercase())
            field(name = "failure.origin", value = "${failure.origin.kind}:${failure.origin.subject}")
            field(name = "failure.thread", value = failure.thread)
            field(name = "failure.sink", value = report.sink)
            MESSAGE
        }
    }
}
