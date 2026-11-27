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

import com.davils.kheck.api.internal.orderedCopy
import com.davils.kheck.api.record.LogValue
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * The fields that [withLogContext] adds to every record logged inside its block, as an element of the coroutine
 * context.
 *
 * The element is created by [withLogContext] and [logContext]. On the JVM the fields follow the coroutine from thread
 * to thread, so a call of a logger that does not suspend still sees them. On WebAssembly a call of a logger
 * cannot read the context of its coroutine, so the fields reach no record there; pass them as fields of the event
 * instead.
 *
 * At most [Limits.MAX_FIELDS][com.davils.kheck.api.Limits.MAX_FIELDS] fields are kept; blank names and names longer
 * than [Limits.MAX_NAME_LENGTH][com.davils.kheck.api.Limits.MAX_NAME_LENGTH] are left out.
 *
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LogContext internal constructor(fields: Map<String, LogValue>) : AbstractCoroutineContextElement(Key) {
    /**
     * The fields of the context; the map cannot be changed.
     *
     * @since 1.0.0
     */
    public val fields: Map<String, LogValue> = fields.bounded().orderedCopy()

    /**
     * Shows the names of the fields, escaped, not their values.
     *
     * @return the context as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LogContext(fields=${fields.keys.map { name -> SafeText.text(name) }})"

    /**
     * The key of the element in a coroutine context.
     *
     * @since 1.0.0
     */
    public companion object Key : CoroutineContext.Key<LogContext>
}
