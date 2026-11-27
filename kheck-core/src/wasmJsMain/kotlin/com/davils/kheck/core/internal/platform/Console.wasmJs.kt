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

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.davils.kheck.core.internal.platform

private const val LINE_BREAK: String = "\n"

internal actual fun writeOutput(text: String) {
    consoleLog(text.removeSuffix(LINE_BREAK))
}

internal actual fun writeError(text: String) {
    consoleError(text.removeSuffix(LINE_BREAK))
}

internal actual fun isConsoleInteractive(): Boolean = false

internal actual fun processIdOrNull(): Long? = null

@JsFun("(text) => console.log(text)")
private external fun consoleLog(text: String)

@JsFun("(text) => console.error(text)")
private external fun consoleError(text: String)
