/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.smithy.kotlin.runtime.client

import aws.smithy.kotlin.runtime.util.Attributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Per operation metadata a service client uses to drive the execution of a single request/response
 */
public class ExecutionContext private constructor(builder: ExecutionContextBuilder) : Attributes by builder.attributes, CoroutineScope {
    /**
     * Default construct an [ExecutionContext]. Note: this is not usually useful without configuring the call attributes
     */
    public constructor() : this(ExecutionContextBuilder())

    override val coroutineContext: CoroutineContext = Job()

    /**
     * Attributes associated with this particular execution/call
     */
    public val attributes: Attributes = builder.attributes

    public companion object {
        public fun build(block: ExecutionContextBuilder.() -> Unit): ExecutionContext = ExecutionContextBuilder().apply(block).build()
    }

    public class ExecutionContextBuilder {
        public var attributes: Attributes = Attributes()

        public fun build(): ExecutionContext = ExecutionContext(this)
    }
}
