package dev.tmsoft.lib.openapi.ktor

import dev.tmsoft.lib.openapi.OpenApiKType
import dev.tmsoft.lib.openapi.Type
import dev.tmsoft.lib.openapi.openApiKType
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.response.respondText
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import kotlin.reflect.KType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import dev.tmsoft.lib.openapi.OpenAPI as SwaggerOpenAPI

class OpenAPI(configuration: Configuration) {
    private val logger by lazy { LoggerFactory.getLogger(OpenAPI::class.java) }
    private val typeBuilder: (OpenApiKType) -> Type.Object = configuration.typeBuilder
    private val responseBuilder: (OpenApiKType) -> Map<Int, Type> = configuration.responseBuilder
    private val documentationBuilder: SwaggerOpenAPI = configuration.documentationBuilder
    private val path: String = configuration.path
    private val json = Json {
        encodeDefaults = false
    }

    class Configuration {
        var typeBuilder: (OpenApiKType) -> Type.Object = { type -> type.objectType("response") }
        var responseBuilder: (OpenApiKType) -> Map<Int, Type> =
            { type -> mapOf(HttpStatusCode.OK.value to type.type()) }
        var path = "/openapi.json"
        var configure: (SwaggerOpenAPI) -> Unit = {}
        var documentationBuilder: SwaggerOpenAPI = SwaggerOpenAPI("localhost")
    }

    fun addToPath(
        path: String,
        method: HttpMethod,
        response: KType? = null,
        body: KType? = null,
        pathParams: KType? = null
    ) {
        documentationBuilder.addToPath(
            path,
            SwaggerOpenAPI.Method.valueOf(method.value),
            response?.openApiKType?.let(responseBuilder) ?: emptyMap(),
            body?.openApiKType?.let(typeBuilder),
            pathParams?.openApiKType?.let(typeBuilder)
        )
    }

    fun addToPath(
        path: String,
        method: SwaggerOpenAPI.Method,
        responses: Map<Int, Type> = emptyMap(),
        body: Type.Object? = null,
        pathParams: Type.Object? = null
    ) {
        documentationBuilder.addToPath(
            path,
            method,
            responses,
            body,
            pathParams
        )
    }


    suspend fun intercept(
        context: PipelineContext<Unit, ApplicationCall>
    ) {
        if (context.call.request.path() == path) {
            try {
                val response = json.encodeToString(documentationBuilder.root)
                context.call.response.status(HttpStatusCode.OK)

                context.call.respondText(response, contentType = ContentType.Application.Json)
                context.finish()
            } catch (ignore: Throwable) {
                logger.error(ignore.message)
            }
        }
    }

    /**
     * Installable feature for [OpenAPI].
     */
    companion object Feature :
        ApplicationFeature<ApplicationCallPipeline, Configuration, dev.tmsoft.lib.openapi.ktor.OpenAPI> {
        override val key = AttributeKey<dev.tmsoft.lib.openapi.ktor.OpenAPI>("OpenApi")
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): dev.tmsoft.lib.openapi.ktor.OpenAPI {
            val configuration = Configuration().apply(configure)
            val feature = OpenAPI(configuration)
            configuration.configure(feature.documentationBuilder)
            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }
            return feature
        }
    }
}
