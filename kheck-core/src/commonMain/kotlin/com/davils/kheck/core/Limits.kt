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

package com.davils.kheck.core

/**
 * The upper bounds the engine of kheck enforces on what it captures and writes.
 *
 * Like the [limits of the API][com.davils.kheck.api.Limits], they never make a call of a logger fail: what exceeds a
 * bound is left out or shortened, and that is said where it happens, by a count of omitted frames, a note in the
 * stack trace or a mark at the end of a shortened text.
 *
 * The limits are part of the public contract: raising one is a compatible change, lowering one a breaking change.
 *
 * @since 1.0.0
 */
public object Limits {
    /**
     * The longest text a layout writes for one message, field value or name, in UTF-16 code units; a longer text is
     * shortened and ends with `…(+n chars)`.
     *
     * @since 1.0.0
     */
    public const val MAX_TEXT_LENGTH: Int = 65_536

    /**
     * The most frames captured from the stack of one throwable; the rest is counted in
     * [LogCause.omittedFrames][com.davils.kheck.core.record.LogCause.omittedFrames].
     *
     * @since 1.0.0
     */
    public const val MAX_CAPTURED_FRAMES: Int = 1_024

    /**
     * The longest chain of causes and suppressed throwables captured from one throwable.
     *
     * @since 1.0.0
     */
    public const val MAX_CAUSE_DEPTH: Int = 16

    /**
     * The most suppressed throwables captured from one throwable.
     *
     * @since 1.0.0
     */
    public const val MAX_SUPPRESSED: Int = 16

    /**
     * The largest capacity of a queue, of the intake or of a sink.
     *
     * @since 1.0.0
     */
    public const val MAX_CAPACITY: Int = 1_048_576

    /**
     * The most loggers, sinks and filters one configuration declares, each.
     *
     * @since 1.0.0
     */
    public const val MAX_DECLARATIONS: Int = 1_024

    /**
     * The most distinct loggers one logging hands out; a logger beyond it is created but not remembered, so it does
     * not follow a change of levels made after its creation.
     *
     * @since 1.0.0
     */
    public const val MAX_LOGGERS: Int = 65_536

    /**
     * The most distinct names of masking rules that the statistics count separately.
     *
     * @since 1.0.0
     */
    public const val MAX_COUNTED_RULES: Int = 256
}
