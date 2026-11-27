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

package com.davils.kheck.core.context

import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.internal.platform.boundContextFields
import com.davils.kheck.core.internal.platform.replaceBoundContextFields
import com.davils.kore.core.annotation.DavilsInternal

/**
 * Reads and replaces the fields of the log context bound to the current thread, for bridges such as the MDC of SLF4J.
 *
 * On WebAssembly no fields are bound to a thread: [current] is always empty and [replace] does nothing.
 *
 * @since 1.0.0
 */
@DavilsInternal
public object ContextAccess {
    /**
     * Returns the fields bound to the current thread.
     *
     * @return the fields, empty when none are bound.
     * @since 1.0.0
     */
    public fun current(): Map<String, LogValue> = boundContextFields()

    /**
     * Binds [fields] to the current thread in place of the fields bound before.
     *
     * @param fields the fields to bind.
     * @return the fields bound before.
     * @since 1.0.0
     */
    public fun replace(fields: Map<String, LogValue>): Map<String, LogValue> {
        val bounded = fields.bounded()
        return replaceBoundContextFields(bounded)
    }
}
