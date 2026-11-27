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

import com.davils.kheck.api.Limits
import com.davils.kheck.api.record.LogFieldsBuilder
import com.davils.kheck.api.record.LogValue
import com.davils.kheck.core.internal.platform.boundContextFields
import com.davils.kheck.core.internal.platform.contextBinding
import com.davils.kore.core.annotation.DavilsInternal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Runs [block] with the fields [fields] adds in the log context, so that every record logged inside it carries them.
 *
 * The new fields are added to the fields of an enclosing context; a field of the same name replaces the outer one
 * inside the block. The fields are limited like the fields of an event, as
 * [LogFieldsBuilder][com.davils.kheck.api.record.LogFieldsBuilder] describes. See [LogContext] for the platforms.
 *
 * @param T the result of [block].
 * @param fields adds the fields of the context.
 * @param block the code that runs inside the context.
 * @return the result of [block].
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public suspend fun <T> withLogContext(fields: LogFieldsBuilder.() -> Unit, block: suspend CoroutineScope.() -> T): T {
    val inherited = currentCoroutineContext()[LogContext]?.fields ?: boundContextFields()
    val builder = LogFieldsBuilder()
    builder.fields()
    return withContext(logContext(inherited + builder.collected()), block)
}

/**
 * Creates a coroutine context that adds [fields] to every record logged inside it, in place of the fields of an
 * enclosing context.
 *
 * The context also binds the fields on the JVM, so a call of a logger that does not suspend sees them on every thread
 * the coroutine runs on, and so does the MDC of SLF4J. Use it with `withContext`, `launch` or `async`;
 * [withLogContext] adds to the enclosing fields instead of replacing them.
 *
 * At most [Limits.MAX_FIELDS] fields are kept; blank names and names longer than [Limits.MAX_NAME_LENGTH] are left
 * out. On WebAssembly the fields reach no record, as described at [LogContext].
 *
 * @param fields the fields of the context, copied.
 * @return the context, made of a [LogContext] and its binding.
 * @since 1.0.0
 */
public fun logContext(fields: Map<String, LogValue>): CoroutineContext {
    val element = LogContext(fields)
    return element + contextBinding(element.fields)
}

/**
 * Creates a coroutine context with the fields that [fields] adds, as [logContext] with a map does.
 *
 * @param fields adds the fields of the context; what exceeds the limits of [Limits] is left out.
 * @return the context, made of a [LogContext] and its binding.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public fun logContext(fields: LogFieldsBuilder.() -> Unit): CoroutineContext {
    val builder = LogFieldsBuilder()
    builder.fields()
    return logContext(builder.collected())
}
