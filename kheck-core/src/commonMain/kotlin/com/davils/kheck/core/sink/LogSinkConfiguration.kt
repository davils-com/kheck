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

package com.davils.kheck.core.sink

import com.davils.kore.core.validation.Validatable

/**
 * The configuration of one sink: its shared [options] and whatever the kind of sink adds.
 *
 * @since 1.0.0
 */
public interface LogSinkConfiguration : Validatable {
    /**
     * The options every sink shares.
     *
     * @since 1.0.0
     */
    public val options: LogSinkOptions
}
