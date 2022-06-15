// TODO: @shustrik
// package dev.tmsoft.lib.ktor.openapi
//
// import com.turbomates.openapi.OpenApiKType
// import com.turbomates.openapi.Property
// import com.turbomates.openapi.Type
// import dev.tmsoft.lib.ktor.Response
// import kotlin.reflect.full.isSubclassOf
// import kotlin.reflect.full.isSubtypeOf
// import kotlin.reflect.full.memberProperties
// import kotlin.reflect.typeOf
// import kotlinx.serialization.json.buildJsonObject
// import kotlinx.serialization.json.put
//
// class DescriptionBuilder(private val type: OpenApiKType) {
//     fun buildResponseMap(): Map<Int, Type> {
//         return when {
//             type.jvmErasure.isSubclassOf(Response.Ok::class) -> mapOf(
//                 HttpStatus.OK.code to getOkType()
//             )
//             type.jvmErasure.isSubclassOf(Response.Either::class) -> buildEitherResponseMap()
//             type.jvmErasure.isSubclassOf(Response.Listing::class) -> mapOf(
//                 HttpStatus.OK.code to buildType()
//             )
//             type.jvmErasure.isSubclassOf(Response.Error::class) -> mapOf(
//                 HttpStatus.UnprocessableEntity.code to getErrorType()
//             )
//             type.jvmErasure.isSubclassOf(Response.Errors::class) -> mapOf(
//                 HttpStatus.UnprocessableEntity.code to buildType()
//             )
//             type.jvmErasure.isSubclassOf(Response.Data::class) -> mapOf(
//                 HttpStatus.OK.code to buildType()
//             )
//             else -> mapOf(
//                 HttpStatus.OK.code to type.type()
//             )
//         }
//     }
//
//     fun buildType(): Type.Object {
//         return type.objectType(type.jvmErasure.simpleName!!)
//     }
//
//     private fun getErrorType(): Type {
//         return Type.Object(
//             "error",
//             listOf(
//                 Property(
//                     "error",
//                     Type.String()
//                 )
//             ),
//             example = buildJsonObject { put("error", "Wrong response") }
//         )
//     }
//
//     private fun getOkType(): Type {
//         return Type.Object(
//             "ok",
//             listOf(
//                 Property(
//                     "data",
//                     Type.String()
//                 )
//             ),
//             example = buildJsonObject { put("data", "ok") }
//         )
//     }
//
//     private fun buildEitherResponseMap(): Map<Int, Type> {
//         val data = type.jvmErasure.memberProperties.first()
//         val result = mutableMapOf<Int, Type>()
//         data.returnType.arguments.forEach { argument ->
//             var projectionType = type.getArgumentProjectionType(argument.type!!)
//             when {
//                 projectionType.type.isSubtypeOf(typeOf<Response.Ok>()) -> {
//                     result[HttpStatus.OK.code] = getOkType()
//                 }
//                 projectionType.type.isSubtypeOf(typeOf<Response.Errors>()) -> {
//                     result[HttpStatus.UnprocessableEntity.code] = projectionType.objectType("errors")
//                 }
//                 projectionType.type.isSubtypeOf(typeOf<Response.Error>()) -> {
//                     result[HttpStatus.UnprocessableEntity.code] = getErrorType()
//                 }
//                 else -> {
//                     result[HttpStatus.OK.code] = projectionType.objectType(projectionType.jvmErasure.simpleName!!)
//                 }
//             }
//         }
//         return result
//     }
//
//     enum class HttpStatus(val code: Int) {
//         OK(200),
//         UnprocessableEntity(422)
//     }
// }
