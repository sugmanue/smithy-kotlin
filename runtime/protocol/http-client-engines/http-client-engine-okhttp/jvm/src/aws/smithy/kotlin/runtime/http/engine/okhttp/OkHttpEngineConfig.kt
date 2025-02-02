/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.smithy.kotlin.runtime.http.engine.okhttp

import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig

public class OkHttpEngineConfig private constructor(builder: Builder) : HttpClientEngineConfig(builder) {
    public companion object {
        /**
         * The default engine config. Most clients should use this.
         */
        public val Default: OkHttpEngineConfig = OkHttpEngineConfig(Builder())

        public operator fun invoke(block: Builder.() -> Unit): OkHttpEngineConfig =
            Builder().apply(block).build()
    }

    public class Builder : HttpClientEngineConfig.Builder() {

        internal fun build(): OkHttpEngineConfig = OkHttpEngineConfig(this)
    }
}
