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

import com.davils.kheck.api.record.LogFieldsBuilder
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.internal.platform.processIdOrNull
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal

/**
 * Adds fields to the context of every record inside `enrich { }`, such as the name of the service or the host.
 *
 * The fields are read once, when the configuration is built, not per record.
 *
 * @since 1.0.0
 */
@DavilsDsl
@OptIn(DavilsInternal::class)
public class EnrichmentBuilder internal constructor(private val environment: Map<String, String>) : LogFieldsBuilder() {
    /**
     * Adds the name of the host from `HOSTNAME` or `COMPUTERNAME`, or `unknown`.
     *
     * @param name the name of the field; `host` by default.
     * @since 1.0.0
     */
    public fun hostname(name: String = HOSTNAME) {
        val host = environment[HOSTNAME_VARIABLE] ?: environment[COMPUTER_NAME_VARIABLE] ?: UNKNOWN
        field(name = name, value = host)
    }

    /**
     * Adds the id of the process; nothing on WebAssembly, which has none.
     *
     * @param name the name of the field; `pid` by default.
     * @since 1.0.0
     */
    public fun processId(name: String = PROCESS_ID) {
        val id = processIdOrNull() ?: return
        field(name = name, value = id)
    }

    /**
     * Adds the name of the user from `USER` or `USERNAME`, or `unknown`.
     *
     * @param name the name of the field; `user` by default.
     * @since 1.0.0
     */
    public fun user(name: String = USER) {
        val user = environment[USER_VARIABLE] ?: environment[USER_NAME_VARIABLE] ?: UNKNOWN
        field(name = name, value = user)
    }

    internal fun fields(): Map<String, LogValue> = collected()

    private companion object {
        private const val HOSTNAME: String = "host"
        private const val PROCESS_ID: String = "pid"
        private const val USER: String = "user"
        private const val HOSTNAME_VARIABLE: String = "HOSTNAME"
        private const val COMPUTER_NAME_VARIABLE: String = "COMPUTERNAME"
        private const val USER_VARIABLE: String = "USER"
        private const val USER_NAME_VARIABLE: String = "USERNAME"
        private const val UNKNOWN: String = "unknown"
    }
}
