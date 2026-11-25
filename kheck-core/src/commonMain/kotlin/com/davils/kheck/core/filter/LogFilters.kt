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

package com.davils.kheck.core.filter

import com.davils.kheck.api.LogLevel
import com.davils.kheck.api.LoggerName
import com.davils.kheck.api.record.LogMarker

/**
 * The common filters.
 *
 * @since 1.0.0
 */
public object LogFilters {
    /**
     * Passes records of [level] and above.
     *
     * @param level the lowest level that passes.
     * @return the filter.
     * @since 1.0.0
     */
    public fun atLeast(level: LogLevel): LogFilter = LogFilter { record -> record.level.isAtLeast(level) }

    /**
     * Passes records that carry at least one of [markers].
     *
     * @param markers the markers, copied.
     * @return the filter.
     * @since 1.0.0
     */
    public fun marked(markers: Set<LogMarker>): LogFilter {
        val accepted = markers.toSet()
        return LogFilter { record -> record.markers.any { marker -> marker in accepted } }
    }

    /**
     * Passes records that carry none of [markers].
     *
     * @param markers the markers, copied.
     * @return the filter.
     * @since 1.0.0
     */
    public fun unmarked(markers: Set<LogMarker>): LogFilter {
        val refused = markers.toSet()
        return LogFilter { record -> record.markers.none { marker -> marker in refused } }
    }

    /**
     * Passes records of [logger] and of the loggers below it.
     *
     * @param logger the dotted name of the logger.
     * @return the filter.
     * @throws IllegalArgumentException if [logger] is longer than the limit of a name.
     * @since 1.0.0
     */
    public fun within(logger: String): LogFilter {
        val ancestor = LoggerName(logger)
        return LogFilter { record -> record.source.logger.isWithin(ancestor) }
    }

    /**
     * Passes records of every logger except [logger] and the loggers below it.
     *
     * @param logger the dotted name of the logger.
     * @return the filter.
     * @throws IllegalArgumentException if [logger] is longer than the limit of a name.
     * @since 1.0.0
     */
    public fun outside(logger: String): LogFilter {
        val ancestor = LoggerName(logger)
        return LogFilter { record -> !record.source.logger.isWithin(ancestor) }
    }
}
