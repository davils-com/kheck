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

import com.davils.kheck.core.Logging
import com.davils.knot.shutdown.Shutdown
import com.davils.knot.shutdown.ShutdownPlanBuilder
import com.davils.knot.shutdown.ShutdownRegistration
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val CLOSE_SUFFIX: String = "-close"
private const val DEFAULT_NAME: String = "logging"
private val closeMargin: Duration = 1.seconds

/**
 * Closes [logging] as a task of the shutdown, in the last phase, `Flush`, so the records of every earlier task, of
 * the failures and of the shutdown itself are still written.
 *
 * The task is named `<name>-close`. It waits at most [timeout] for the sinks and leaves the margin of one second to
 * the timeout of the task; a sink that hangs is cancelled and its waiting records are counted as dropped after close.
 * kheck registers no hook of its own: the application installs the shutdown, such as with `Shutdown.install`.
 *
 * @receiver the plan of the shutdown.
 * @param logging the logging to close.
 * @param timeout the longest wait for the sinks.
 * @param name the name of the task without its suffix; `logging` by default.
 * @since 1.0.0
 */
public fun ShutdownPlanBuilder<*>.logging(
    logging: Logging,
    timeout: Duration = 5.seconds,
    name: String = DEFAULT_NAME,
) {
    flush {
        task(name + CLOSE_SUFFIX) {
            this.timeout = timeout + closeMargin
            action {
                val budget = minOf(a = timeout, b = remaining - closeMargin)
                logging.close(budget.coerceAtLeast(Duration.ZERO))
            }
        }
    }
}

/**
 * Registers the closing of [logging] with a running shutdown, as [logging] on the plan does.
 *
 * @receiver the shutdown.
 * @param logging the logging to close.
 * @param timeout the longest wait for the sinks.
 * @param name the name of the task without its suffix; `logging` by default.
 * @return the registration, or why the shutdown refused it, such as when it already runs.
 * @since 1.0.0
 */
public fun Shutdown.adopt(
    logging: Logging,
    timeout: Duration = 5.seconds,
    name: String = DEFAULT_NAME,
): ShutdownRegistration = register { logging(logging = logging, timeout = timeout, name = name) }
