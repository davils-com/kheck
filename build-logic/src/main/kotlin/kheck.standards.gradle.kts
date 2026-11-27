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


import java.time.Duration

plugins {
    id("kheck.base")
}

kreate {
    assurance {
        standards {
            enabled = true
            reviewEvery = Duration.ofDays(365)

            inventory {
                includes.set(setOf("*.md", "kheck-*/src/**"))
            }

            rfc(8259) {
                note = "JSON: the bounded writer behind the kheck, ECS, Logstash and OpenTelemetry layouts."
            }
            rfc(3629) {
                note = "UTF-8: the bytes every sink writes and the escaping of lone surrogates."
            }
            rfc(3339) {
                note = "Date and time on the Internet: the timestamps of the JSON layouts and of the plain layout."
            }
            rfc(2104) {
                note = "HMAC: the masking hash and the seals of the integrity chains, both computed by krypt."
            }
            rfc(4231) {
                note = "The HMAC-SHA-256 test vectors that pin the masking hash computed by krypt to its 1.0 output."
            }
            document("ECMA-48") {
                note = "Control functions for coded character sets: the SGR sequences of the ANSI console styles."
                url = "https://ecma-international.org/publications-and-standards/standards/ecma-48/"
            }
            github("elastic/ecs") {
                note = "The Elastic Common Schema: the field names and types of the ECS layout."
                tags = """^v\d+\.\d+\.\d+$"""
            }
            github("open-telemetry/opentelemetry-specification") {
                note = "The logs data model: severity numbers and texts, attributes and the scope of the OTel layout."
                tags = """^v\d+\.\d+\.\d+$"""
            }
            github("logfellow/logstash-logback-encoder") {
                note = "The field names of the Logstash JSON event that the Logstash layout writes."
                tags = """^logstash-logback-encoder-\d+\.\d+$"""
            }
            github("qos-ch/slf4j") {
                note = "SLF4J 2: the service provider, MDC, markers and key value pairs that kheck-slf4j implements."
                tags = """^v_\d+\.\d+\.\d+$"""
            }
            iso("13616") {
                note = "IBAN: the mod-97 check that the masking validator of account numbers applies."
            }
            iso("7812") {
                note = "Card numbers: the Luhn check that the masking validator of payment cards applies."
            }
            iso("8601") {
                note = "Date and time: the instants and durations that the layouts write."
            }
            unicode {
                note = "Control, format and bidirectional characters are escaped at every sink, as the platform defines them."
            }
        }
    }
}
