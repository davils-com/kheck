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

import com.davils.kore.core.validation.report.ValidationReport

/**
 * The outcome of [Logging.reconfigure].
 *
 * @since 1.0.0
 */
public sealed interface Reconfiguration {
    /**
     * The new configuration is in effect; records accepted before it were written with the old one.
     *
     * @since 1.0.0
     */
    public object Applied : Reconfiguration {
        /**
         * Shows the name of the outcome.
         *
         * @return `Applied`.
         * @since 1.0.0
         */
        override fun toString(): String = "Applied"
    }

    /**
     * The new configuration was invalid or changed the intake; the old one stays.
     *
     * @property report why the configuration was refused.
     * @since 1.0.0
     */
    public class Rejected internal constructor(public val report: ValidationReport) : Reconfiguration {
        /**
         * Shows the count of errors.
         *
         * @return the outcome as text.
         * @since 1.0.0
         */
        override fun toString(): String = "Rejected(errors=${report.errors.size})"
    }

    /**
     * The logging is closed and takes no configuration.
     *
     * @since 1.0.0
     */
    public object Closed : Reconfiguration {
        /**
         * Shows the name of the outcome.
         *
         * @return `Closed`.
         * @since 1.0.0
         */
        override fun toString(): String = "Closed"
    }
}
