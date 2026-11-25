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

plugins {
    id("kheck.kotlin-library")
}

kreate {
    project {
        description = "Test support for kheck: recording loggers, capturing sinks, fake terminals, record builders " +
            "and recorded diagnostics, for the JVM and WebAssembly; never on a production class path."
    }
}

dependencies {
    commonMainApi(project(":kheck-core"))
    commonMainApi(libs.kore.testing)
}

val testConfigurations: Set<String> = setOf("commonUnitTestImplementation", "wasmJsTestImplementation")

configurations.matching { configuration -> configuration.name in testConfigurations }.configureEach {
    dependencies.add(project.dependencies.create(libs.kotlinx.coroutines.test.get()))
}
