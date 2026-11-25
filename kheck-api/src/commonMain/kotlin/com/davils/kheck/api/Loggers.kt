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

package com.davils.kheck.api

import com.davils.kheck.api.record.LogEventBuilder

/**
 * Logs a record of [level] whose message and fields [event] builds, but only if [level] is enabled.
 *
 * [event] runs on the calling thread, at once, and only when the record will be written, so building the message
 * costs nothing for a disabled level. It returns the message and may add fields, a cause and markers.
 *
 * @param level the level of the record; [LogLevel.Off] logs nothing.
 * @param event builds the record and returns its message.
 * @since 1.0.0
 */
public inline fun Logger.log(level: LogLevel, event: LogEventBuilder.() -> String) {
    if (!isEnabled(level)) return
    val builder = LogEventBuilder()
    val message = builder.event()
    emit(level, builder.build(message))
}

/**
 * Logs a record of [LogLevel.Trace], as [log] describes.
 *
 * @param event builds the record and returns its message.
 * @since 1.0.0
 */
public inline fun Logger.trace(event: LogEventBuilder.() -> String) {
    log(LogLevel.Trace, event)
}

/**
 * Logs a record of [LogLevel.Debug], as [log] describes.
 *
 * @param event builds the record and returns its message.
 * @since 1.0.0
 */
public inline fun Logger.debug(event: LogEventBuilder.() -> String) {
    log(LogLevel.Debug, event)
}

/**
 * Logs a record of [LogLevel.Info], as [log] describes.
 *
 * @param event builds the record and returns its message.
 * @since 1.0.0
 */
public inline fun Logger.info(event: LogEventBuilder.() -> String) {
    log(LogLevel.Info, event)
}

/**
 * Logs a record of [LogLevel.Warn], as [log] describes.
 *
 * @param event builds the record and returns its message.
 * @since 1.0.0
 */
public inline fun Logger.warn(event: LogEventBuilder.() -> String) {
    log(LogLevel.Warn, event)
}

/**
 * Logs a record of [LogLevel.Error], as [log] describes.
 *
 * @param event builds the record and returns its message.
 * @since 1.0.0
 */
public inline fun Logger.error(event: LogEventBuilder.() -> String) {
    log(LogLevel.Error, event)
}
