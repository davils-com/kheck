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

package com.davils.kheck.core.mask

/**
 * Confirms a match of a [MaskingRule] before it is masked, such as the check digit of a card number, so that a number
 * that merely looks like one stays visible.
 *
 * It is called from the coroutine of the logging and must not throw: a validator that throws fails closed, so the
 * record is dropped and the failure reported to the diagnostics.
 *
 * @since 1.0.0
 */
public fun interface MaskingValidator {
    /**
     * Returns whether [candidate] is really a sensitive value.
     *
     * @param candidate the text the pattern matched.
     * @return `true` to mask [candidate].
     * @since 1.0.0
     */
    public fun isMatch(candidate: String): Boolean
}
