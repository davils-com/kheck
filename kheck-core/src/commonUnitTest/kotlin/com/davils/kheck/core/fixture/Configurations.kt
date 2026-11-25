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

package com.davils.kheck.core.fixture

import com.davils.kheck.core.LoggingBuilder
import com.davils.kheck.core.LoggingConfiguration
import com.davils.kore.core.validation.report.ValidationReport
import com.davils.kore.core.validation.violation.ViolationCode

internal fun loggingConfiguration(block: LoggingBuilder.() -> Unit): LoggingConfiguration {
    val builder = LoggingBuilder()
    builder.block()
    return builder.build()
}

internal fun loggingReport(block: LoggingBuilder.() -> Unit): ValidationReport {
    val builder = LoggingBuilder()
    builder.block()
    return builder.validate()
}

internal fun loggingCodes(block: LoggingBuilder.() -> Unit): List<ViolationCode> {
    val report = loggingReport(block)
    return report.errors.map { violation -> violation.code }
}
