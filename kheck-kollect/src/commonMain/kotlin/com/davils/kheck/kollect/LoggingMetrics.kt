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

package com.davils.kheck.kollect

import com.davils.kollect.core.ObservedFamily
import com.davils.kollect.core.Registration
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.immutableList

/**
 * The meters [bindLogging] declared; closing it removes them from the registry.
 *
 * @param registrations the registrations of the observed families, copied.
 * @since 1.0.0
 */
@OptIn(DavilsInternal::class)
public class LoggingMetrics internal constructor(registrations: List<Registration<ObservedFamily>>) : AutoCloseable {
    /**
     * The registrations, one per family; the list cannot be changed.
     *
     * @since 1.0.0
     */
    public val registrations: List<Registration<ObservedFamily>> = registrations.immutableList()

    /**
     * Closes every family, so the registry no longer reports it.
     *
     * @since 1.0.0
     */
    override fun close() {
        registrations.forEach { registration -> registration.family.close() }
    }

    /**
     * Shows the count of families.
     *
     * @return the meters as text.
     * @since 1.0.0
     */
    override fun toString(): String = "LoggingMetrics(${registrations.size} families)"
}
