/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Logging primitives for Smithy services generated by smithy-kotlin"
extra["displayName"] = "Smithy :: Kotlin :: Logging"
extra["moduleName"] = "aws.smithy.kotlin.runtime.logging"


val kotlinLoggingVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":runtime:utils"))
                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
        }
    }
}
