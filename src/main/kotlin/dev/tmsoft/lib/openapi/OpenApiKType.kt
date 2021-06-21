package dev.tmsoft.lib.openapi

import java.util.Locale
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class OpenApiKType(val type: KType) {
    val jvmErasure = type.jvmErasure
    private val projectionTypes: Map<String, KType> = buildGenericTypes(type)
    private fun buildGenericTypes(type: KType): Map<String, KType> {
        val types = mutableMapOf<String, KType>()
        type.jvmErasure.typeParameters.forEachIndexed { index, kTypeParameter ->
            types[kTypeParameter.name] = type.arguments[index].type!!
        }

        return types
    }

    fun type(): Type {
        return buildType(type)
    }

    fun objectType(name: String): Type.Object {
        if (type.isCollection() || type.isPrimitive()) {
            throw InvalidTypeForOpenApiType(type.javaType.typeName, Type.Object::class.simpleName!!)
        }
        return buildType(name, type) as Type.Object
    }

    private fun buildType(name: String, type: KType): Type {
        if (type.isCollection() || type.isPrimitive() || type.isEnum()) {
            return buildType(type)
        }

        val kclass = type.classifier as? KClass<*>
        if (kclass != null && kclass.isValue) {
            return buildType(type.jvmErasure.memberProperties.first().returnType)
        }
        val descriptions = mutableListOf<Property>()
        type.jvmErasure.memberProperties.forEach { property ->
            var memberType = property.returnType
            // ToDo think about parametrization of this option
            if (!property.isLateinit) {
                descriptions.add(Property(property.name, buildType(memberType)))
            }
        }
        return Type.Object(name, descriptions, returnType = type.jvmErasure.qualifiedName)
    }

    private fun buildType(memberType: KType): Type {
        return when {
            memberType.isCollection() -> {
                var collectionType = if (memberType.arguments.isEmpty()) {
                    memberType.jvmErasure.supertypes.first {
                        it.isSubtypeOf(typeOf<Set<*>>()) || it.isSubtypeOf(typeOf<List<*>>())
                    }.arguments.first().type!!
                } else {
                    memberType.arguments.first().type!!
                }
                if (projectionTypes.containsKey(collectionType.toString())) {
                    collectionType = projectionTypes.getValue(collectionType.toString())
                }
                when {
                    collectionType.isPrimitive() -> Type.Array(collectionType.openApiType)
                    collectionType.isEnum() -> {
                        Type.Array(buildType(collectionType))
                    }
                    else -> Type.Array(
                        buildType(collectionType.jvmErasure.simpleName!!, collectionType)
                    )
                }
            }
            memberType.isMap() -> {
                var firstType = memberType.arguments[0].type!!
                if (projectionTypes.containsKey(firstType.toString())) {
                    firstType = projectionTypes.getValue(firstType.toString())
                }
                var secondType = memberType.arguments[1].type!!
                if (projectionTypes.containsKey(secondType.toString())) {
                    secondType = projectionTypes.getValue(secondType.toString())
                }
                Type.Object(
                    "map",
                    properties = listOf(
                        Property(
                            firstType.jvmErasure.simpleName!!,
                            buildType(secondType)
                        )
                    )
                )
            }
            memberType.isEnum() -> {
                val values = memberType.jvmErasure.java.enumConstants
                Type.String(values.map { it.toString() })
            }
            memberType.isPrimitive() ->
                memberType.openApiType
            else -> {
                var projectionType = memberType
                if (projectionTypes.containsKey(memberType.toString())) {
                    projectionType = projectionTypes.getValue(memberType.toString())
                }
                buildType(projectionType.jvmErasure.simpleName!!, projectionType)
            }
        }
    }

    private fun KType.isPrimitive(): Boolean {
        return javaClass.isPrimitive ||
            isSubtypeOf(typeOf<String?>()) ||
            isSubtypeOf(typeOf<Int?>()) ||
            isSubtypeOf(typeOf<Float?>()) ||
            isSubtypeOf(typeOf<Double?>()) ||
            isSubtypeOf(typeOf<Boolean?>()) ||
            isSubtypeOf(typeOf<UUID?>())
    }

    private fun KType.isCollection(): Boolean {
        return isSubtypeOf(typeOf<Collection<*>?>())
    }

    private fun KType.isMap(): Boolean {
        return isSubtypeOf(typeOf<Map<*, *>>())
    }

    private fun KType.isEnum(): Boolean {
        return this.javaClass.isEnum || isSubtypeOf(typeOf<Enum<*>?>())
    }

    fun getArgumentProjectionType(type: KType): OpenApiKType {
        if (projectionTypes.containsKey(type.toString())) {
            return OpenApiKType(projectionTypes.getValue(type.toString()))
        }
        return OpenApiKType(type)
    }

    private val KType.openApiType: Type
        get() {
            return when {
                isSubtypeOf(typeOf<String?>()) -> Type.String()
                isSubtypeOf(typeOf<Locale?>()) -> Type.String()
                isSubtypeOf(typeOf<UUID?>()) -> Type.String()
                isSubtypeOf(typeOf<Int?>()) -> Type.Number
                isSubtypeOf(typeOf<Float?>()) -> Type.Number
                isSubtypeOf(typeOf<Boolean?>()) -> Type.Boolean
                isSubtypeOf(typeOf<Double?>()) -> Type.Number
                else -> throw UnhandledTypeException(jvmErasure.simpleName!!)
            }
        }
}

val KType.openApiKType: OpenApiKType
    get() = OpenApiKType(this)

class UnhandledTypeException(type: String) : Exception("unhandled type $type")
class InvalidTypeForOpenApiType(type: String, openApiType: String) : Exception("Invalid $type to build $openApiType")
