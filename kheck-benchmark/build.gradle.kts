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
    id("kheck.benchmark")
}

kreate {
    project {
        description = "Throughput benchmarks of kheck, the logging library of Davils, measured with JMH on the JVM: " +
            "the call of a disabled logger, the pipeline with the hash mask and the integrity chain, and the " +
            "verification of a chain. Never published."
    }
}

configurations.matching { configuration -> configuration.name == "benchmarksImplementation" }.configureEach {
    dependencies.add(project.dependencies.project(":kheck-core"))
}
