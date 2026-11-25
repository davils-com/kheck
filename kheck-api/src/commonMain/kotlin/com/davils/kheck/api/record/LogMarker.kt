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

package com.davils.kheck.api.record

import com.davils.kheck.api.Limits
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

/**
 * A tag that classifies a record across loggers, such as [AUDIT] or [SECURITY], so that a sink can take exactly the
 * records with that tag.
 *
 * @property name the name of the marker, compared exactly.
 * @throws IllegalArgumentException if [name] is blank or longer than [Limits.MAX_NAME_LENGTH].
 * @since 1.0.0
 */
@JvmInline
public value class LogMarker(public val name: String) {
    init {
        require(name.isNotBlank()) { BLANK }
        require(name.length <= Limits.MAX_NAME_LENGTH) { TOO_LONG }
    }

    /**
     * Shows the name of the marker, escaped.
     *
     * @return the escaped name.
     * @since 1.0.0
     */
    @OptIn(DavilsInternal::class)
    override fun toString(): String = SafeText.text(name)

    /**
     * The markers every Davils application shares.
     *
     * @since 1.0.0
     */
    public companion object {
        private const val BLANK: String = "The name of a marker must not be blank"
        private const val TOO_LONG: String = "The name of a marker must not be longer than ${Limits.MAX_NAME_LENGTH}"

        /**
         * Marks a record that belongs to the audit trail: who did what, and when.
         *
         * @since 1.0.0
         */
        public val AUDIT: LogMarker = LogMarker("AUDIT")

        /**
         * Marks a record about security, such as a failed authentication or a refused request.
         *
         * @since 1.0.0
         */
        public val SECURITY: LogMarker = LogMarker("SECURITY")
    }
}
