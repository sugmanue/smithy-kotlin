/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.smithy.kotlin.codegen.core

import software.amazon.smithy.codegen.core.CodegenException
import software.amazon.smithy.codegen.core.SymbolDependency
import software.amazon.smithy.codegen.core.SymbolDependencyContainer
import software.amazon.smithy.utils.StringUtils

// root namespace for the client-runtime
const val CLIENT_RT_ROOT_NS = "software.aws.clientrt"

/**
 * Test if a string represents a valid artifact version string
 */
fun isValidVersion(version: String): Boolean {
    val re = Regex("\\d\\.\\d\\.\\d[a-z0-9A-Z.-]*\$")
    return re.matches(version)
}

private fun getDefaultRuntimeVersion(): String {
    // generated as part of the build, see smithy-kotlin-codegen/build.gradle.kts
    try {
        val version = object {}.javaClass.getResource("sdk-version.txt").readText()
        check(isValidVersion(version)) { "Version parsed from sdk-version.txt '$version' is not a valid version string" }
        return version
    } catch (ex: Exception) {
        throw CodegenException("failed to load sdk-version.txt which sets the default client-runtime version", ex)
    }
}

// publishing info
const val CLIENT_RT_GROUP: String = "software.aws.smithy.kotlin"
val CLIENT_RT_VERSION: String = System.getProperty("smithy.kotlin.codegen.clientRuntimeVersion", getDefaultRuntimeVersion())
val KOTLIN_COMPILER_VERSION: String = System.getProperty("smithy.kotlin.codegen.kotlinCompilerVersion", "1.5.0")

// See: https://docs.gradle.org/current/userguide/java_library_plugin.html#sec:java_library_configurations_graph
enum class GradleConfiguration {
    // purely internal and not meant to be exposed to consumers.
    Implementation,
    // transitively exported to consumers, for compile.
    Api,
    // only required at compile time, but should not leak into the runtime
    CompileOnly,
    // only required at runtime
    RuntimeOnly,
    // internal test
    TestImplementation,
    // compile time test only
    TestCompileOnly,
    // compile time runtime only
    TestRuntimeOnly;

    override fun toString(): String = StringUtils.uncapitalize(this.name)
}

data class KotlinDependency(
    val config: GradleConfiguration,
    val namespace: String,
    val group: String,
    val artifact: String,
    val version: String
) : SymbolDependencyContainer {

    companion object {
        // AWS managed dependencies
        val CLIENT_RT_CORE = KotlinDependency(GradleConfiguration.Api, CLIENT_RT_ROOT_NS, CLIENT_RT_GROUP, "client-rt-core", CLIENT_RT_VERSION)
        val CLIENT_RT_HTTP = KotlinDependency(GradleConfiguration.Implementation, "$CLIENT_RT_ROOT_NS.http", CLIENT_RT_GROUP, "http", CLIENT_RT_VERSION)
        val CLIENT_RT_SERDE = KotlinDependency(GradleConfiguration.Implementation, "$CLIENT_RT_ROOT_NS.serde", CLIENT_RT_GROUP, "serde", CLIENT_RT_VERSION)
        val CLIENT_RT_SERDE_JSON = KotlinDependency(GradleConfiguration.Implementation, "$CLIENT_RT_ROOT_NS.serde.json", CLIENT_RT_GROUP, "serde-json", CLIENT_RT_VERSION)
        val CLIENT_RT_SERDE_XML = KotlinDependency(GradleConfiguration.Implementation, "$CLIENT_RT_ROOT_NS.serde.xml", CLIENT_RT_GROUP, "serde-xml", CLIENT_RT_VERSION)
        val CLIENT_RT_SERDE_FORM_URL = KotlinDependency(GradleConfiguration.Implementation, "$CLIENT_RT_ROOT_NS.serde.formurl", CLIENT_RT_GROUP, "serde-form-url", CLIENT_RT_VERSION)
        val CLIENT_RT_HTTP_KTOR_ENGINE = KotlinDependency(GradleConfiguration.Implementation, "$CLIENT_RT_ROOT_NS.http.engine.ktor", CLIENT_RT_GROUP, "http-client-engine-ktor", CLIENT_RT_VERSION)
        val CLIENT_RT_UTILS = KotlinDependency(GradleConfiguration.Implementation, "$CLIENT_RT_ROOT_NS.util", CLIENT_RT_GROUP, "utils", CLIENT_RT_VERSION)
        val CLIENT_RT_SMITHY_TEST = KotlinDependency(GradleConfiguration.TestImplementation, "$CLIENT_RT_ROOT_NS.smithy.test", CLIENT_RT_GROUP, "smithy-test", CLIENT_RT_VERSION)

        // External third-party dependencies
        val KOTLIN_TEST = KotlinDependency(GradleConfiguration.TestImplementation, "kotlin.test", "org.jetbrains.kotlin", "kotlin-test", KOTLIN_COMPILER_VERSION)
        val KOTLIN_TEST_JUNIT5 = KotlinDependency(GradleConfiguration.TestImplementation, "kotlin.test.junit5", "org.jetbrains.kotlin", "kotlin-test-junit5", KOTLIN_COMPILER_VERSION)
        val JUNIT_JUPITER_ENGINE = KotlinDependency(GradleConfiguration.TestRuntimeOnly, "org.junit.jupiter", "org.junit.jupiter", "junit-jupiter-engine", "5.4.2")
    }

    override fun getDependencies(): List<SymbolDependency> {
        val dependency = SymbolDependency.builder()
            .dependencyType(config.name)
            .packageName(namespace)
            .version(version)
            .putProperty("dependency", this)
            .build()
        return listOf(dependency)
    }
}