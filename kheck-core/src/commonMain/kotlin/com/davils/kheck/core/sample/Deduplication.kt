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

package com.davils.kheck.core.sample

import kotlin.time.Duration

/**
 * The suppression of repeated records: a record with the same logger, level and message as one written within
 * [window] is dropped, and when the window ends the first record is written again with a field `repeated` that counts
 * the dropped ones.
 *
 * @property window how long a record suppresses its repetitions.
 * @property maxEntries the most distinct records remembered at once; the oldest is forgotten first.
 * @since 1.0.0
 */
public class Deduplication(public val window: Duration, public val maxEntries: Int) {
    /**
     * Shows the window and the most entries.
     *
     * @return the deduplication as text.
     * @since 1.0.0
     */
    override fun toString(): String = "Deduplication(window=$window, maxEntries=$maxEntries)"
}
