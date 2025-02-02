/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.kotlin.codegen.rendering

import software.amazon.smithy.codegen.core.SymbolProvider
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.filterEventStreamErrors
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.isBoxed
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.StreamingTrait

/**
 * Renders Smithy union shapes
 */
class UnionGenerator(
    val model: Model,
    private val symbolProvider: SymbolProvider,
    private val writer: KotlinWriter,
    private val shape: UnionShape,
) {

    /**
     * Renders a Smithy union to a Kotlin sealed class
     */
    fun render() {
        check(!shape.allMembers.values.any { memberShape -> memberShape.memberName.equals("SdkUnknown", true) }) { "generating SdkUnknown would cause duplicate variant for union shape: $shape" }
        val symbol = symbolProvider.toSymbol(shape)
        writer.renderDocumentation(shape)
        writer.renderAnnotations(shape)
        writer.openBlock("public sealed class #T {", symbol)

        // event streams (@streaming union) MAY have variants that target errors.
        // These errors if encountered on the stream will be thrown as an exception rather
        // than showing up as one of the possible events the consumer will see on the stream (Flow<T>).
        val members = shape.filterEventStreamErrors(model)

        members.sortedBy { it.memberName }.forEach {
            writer.renderMemberDocumentation(model, it)
            writer.renderAnnotations(it)
            val variantName = it.unionVariantName()
            val variantSymbol = symbolProvider.toSymbol(it)
            writer.writeInline("public data class #L(val value: #Q) : #Q()", variantName, variantSymbol, symbol)
            when (model.expectShape(it.target).type) {
                ShapeType.BLOB -> {
                    writer.withBlock(" {", "}") {
                        renderHashCode(model, listOf(it), symbolProvider, this)
                        renderEquals(model, listOf(it), variantName, this)
                    }
                }
                else -> writer.write("")
            }
        }

        // generate the unknown which will always be last
        writer.write("public object SdkUnknown : #Q()", symbol)

        members.sortedBy { it.memberName }.forEach {
            val variantName = it.unionVariantName()
            val variantSymbol = symbolProvider.toSymbol(it)

            writer.write("")
            writer.dokka {
                write(
                    """
                        Casts this [#T] as a [#L] and retrieves its [#Q] value. Throws an exception if the [#T] is not a
                        [#L].
                    """.trimIndent(),
                    symbol,
                    variantName,
                    variantSymbol,
                    symbol,
                    variantName,
                )
            }
            writer.write("public fun as#L(): #Q = (this as #T.#L).value", variantName, variantSymbol, symbol, variantName)

            writer.write("")
            writer.dokka {
                write(
                    "Casts this [#T] as a [#L] and retrieves its [#Q] value. Returns null if the [#T] is not a [#L].",
                    symbol,
                    variantName,
                    variantSymbol,
                    symbol,
                    variantName,
                )
            }
            writer.write(
                "public fun as#LOrNull(): #Q? = (this as? #T.#L)?.value",
                variantName,
                variantSymbol,
                symbol,
                variantName,
            )
        }

        writer.closeBlock("}").write("")
    }

    // generate a `hashCode()` implementation
    private fun renderHashCode(
        model: Model,
        sortedMembers: List<MemberShape>,
        symbolProvider: SymbolProvider,
        writer: KotlinWriter,
    ) {
        writer.write("")
        writer.withBlock("override fun hashCode(): #Q {", "}", KotlinTypes.Int) {
            write("return value#L", selectHashFunctionForShape(model, sortedMembers[0], symbolProvider))
        }
    }

    // Return the appropriate hashCode fragment based on ShapeID of member target.
    private fun selectHashFunctionForShape(model: Model, member: MemberShape, symbolProvider: SymbolProvider): String {
        val targetShape = model.expectShape(member.target)
        // also available already in the byMember map
        val targetSymbol = symbolProvider.toSymbol(targetShape)

        return when (targetShape.type) {
            ShapeType.INTEGER ->
                when (targetSymbol.isBoxed) {
                    true -> " ?: 0"
                    else -> ""
                }
            ShapeType.BYTE ->
                when (targetSymbol.isBoxed) {
                    true -> ".toInt() ?: 0"
                    else -> ".toInt()"
                }
            ShapeType.BLOB ->
                if (targetShape.hasTrait<StreamingTrait>()) {
                    // ByteStream
                    ".hashCode() ?: 0"
                } else {
                    // ByteArray
                    ".contentHashCode()"
                }
            else ->
                when (targetSymbol.isBoxed) {
                    true -> ".hashCode() ?: 0"
                    else -> ".hashCode()"
                }
        }
    }

    // generate a `equals()` implementation
    private fun renderEquals(model: Model, sortedMembers: List<MemberShape>, typeName: String, writer: KotlinWriter) {
        writer.write("")
        writer.withBlock("override fun equals(other: #Q?): #Q {", "}", KotlinTypes.Any, KotlinTypes.Boolean) {
            write("if (this === other) return true")
            write("if (other == null || this::class != other::class) return false")
            write("")
            write("other as $typeName")
            write("")

            for (memberShape in sortedMembers) {
                val target = model.expectShape(memberShape.target)
                val memberName = "value"
                if (target is BlobShape && !target.hasTrait<StreamingTrait>()) {
                    writer.write("if (!#1L.contentEquals(other.#1L)) return false", memberName)
                } else {
                    write("if (#1L != other.#1L) return false", memberName)
                }
            }

            write("")
            write("return true")
        }
    }
}
