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

package com.davils.kheck.core.format

/**
 * How a plain layout writes the time of a record.
 *
 * @since 1.0.0
 */
public enum class TimestampFormat {
    /**
     * The local time with milliseconds, such as `14:03:27.512`.
     *
     * @since 1.0.0
     */
    Time,

    /**
     * The local date and time with milliseconds, such as `2026-11-24 14:03:27.512`.
     *
     * @since 1.0.0
     */
    DateTime,

    /**
     * ISO 8601 in the zone of the format with its offset, such as `2026-11-24T14:03:27.512+01:00`.
     *
     * @since 1.0.0
     */
    Iso,

    /**
     * ISO 8601 in UTC, such as `2026-11-24T13:03:27.512Z`.
     *
     * @since 1.0.0
     */
    IsoUtc,

    /**
     * The milliseconds since the epoch, such as `1795525407512`.
     *
     * @since 1.0.0
     */
    EpochMillis,
}
