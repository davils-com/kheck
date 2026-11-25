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

/**
 * The severity of a record, from the most verbose [Trace] to [Error], and [Off], which no record has.
 *
 * The levels are ordered: a logger or a sink with a threshold writes every record whose level [isAtLeast] that
 * threshold. [Off] as a threshold silences everything; a record never has the level [Off].
 *
 * @since 1.0.0
 */
public enum class LogLevel {
    /**
     * The finest detail, such as the steps of an algorithm.
     *
     * @since 1.0.0
     */
    Trace,

    /**
     * Detail that helps to find a problem during development.
     *
     * @since 1.0.0
     */
    Debug,

    /**
     * What happens during normal operation.
     *
     * @since 1.0.0
     */
    Info,

    /**
     * Something unexpected that the program recovered from.
     *
     * @since 1.0.0
     */
    Warn,

    /**
     * A failure that someone has to look at.
     *
     * @since 1.0.0
     */
    Error,

    /**
     * No level at all: as a threshold it silences everything, and no record has it.
     *
     * @since 1.0.0
     */
    Off,
    ;

    /**
     * Returns whether a record of this level passes [threshold].
     *
     * @param threshold the lowest level that passes.
     * @return `true` if this level is not [Off] and is at least as severe as [threshold].
     * @since 1.0.0
     */
    public fun isAtLeast(threshold: LogLevel): Boolean {
        if (this == Off) return false
        return ordinal >= threshold.ordinal
    }
}
