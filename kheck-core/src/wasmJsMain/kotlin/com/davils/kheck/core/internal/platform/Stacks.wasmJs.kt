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

package com.davils.kheck.core.internal.platform

import com.davils.kheck.core.record.LogFrame

private const val UNKNOWN_TYPE: String = "Throwable"

internal actual fun locateCaller(skipped: List<String>): LogFrame? = null

internal actual fun framesOf(throwable: Throwable, limit: Int): StackFrames = StackFrames(emptyList(), omitted = 0)

internal actual fun typeNameOf(throwable: Throwable): String {
    val type = throwable::class
    return type.qualifiedName ?: type.simpleName ?: UNKNOWN_TYPE
}
