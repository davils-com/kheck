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

package com.davils.kheck.core.format.plain

import com.davils.kheck.core.style.SegmentStyle
import com.davils.kore.core.annotation.DavilsDsl

/**
 * Changes single styles of a theme, starting from the styles of the theme it derives from.
 *
 * Not safe for use from several threads; [PlainTheme.derived] uses it once.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class PlainThemeBuilder internal constructor(base: PlainTheme) {
    /**
     * The style of the timestamp.
     *
     * @since 1.0.0
     */
    public var timestamp: SegmentStyle = base.timestamp

    /**
     * The style of the level.
     *
     * @since 1.0.0
     */
    public var level: SegmentStyle = base.level

    /**
     * The style of the thread.
     *
     * @since 1.0.0
     */
    public var thread: SegmentStyle = base.thread

    /**
     * The style of the logger.
     *
     * @since 1.0.0
     */
    public var logger: SegmentStyle = base.logger

    /**
     * The style of the caller.
     *
     * @since 1.0.0
     */
    public var caller: SegmentStyle = base.caller

    /**
     * The style of the markers.
     *
     * @since 1.0.0
     */
    public var markers: SegmentStyle = base.markers

    /**
     * The style of the message.
     *
     * @since 1.0.0
     */
    public var message: SegmentStyle = base.message

    /**
     * The styles of fields and context.
     *
     * @since 1.0.0
     */
    public var entries: EntryStyles = base.entries

    /**
     * The styles of a stack trace.
     *
     * @since 1.0.0
     */
    public var cause: CauseStyles = base.cause

    internal fun theme(): PlainTheme = PlainTheme(
        timestamp = timestamp,
        level = level,
        thread = thread,
        logger = logger,
        caller = caller,
        markers = markers,
        message = message,
        entries = entries,
        cause = cause,
    )
}
