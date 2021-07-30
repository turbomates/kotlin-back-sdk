package dev.tmsoft.lib.openapi.ktor

import dev.tmsoft.lib.openapi.OpenAPI
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

class OpenApi(configuration: Configuration) {
    private val logger by lazy { LoggerFactory.getLogger(OpenApi::class.java) }
    private val typeBuilder: (OpenApiKType) -> Type.Object = configuration.typeBuilder
    private val responseBuilder: (OpenApiKType) -> Map<Int, Type> = configuration.responseBuilder
    private val openApi: OpenAPI = configuration.openApi
    private val path: String = configuration.path

    class Configuration {
        var typeBuilder: (OpenApiKType) -> Type.Object = { type -> type.objectType("response") }
        var responseBuilder: (OpenApiKType) -> Map<Int, Type> = { type -> mapOf(200 to type.type()) }
        var path = "/openapi.json"
        var configure: (OpenAPI) -> Unit = {}
        var openApi: OpenAPI = OpenAPI("localhost")
    }

    fun addToPath(
        path: String,
        method: HttpMethod,
        response: KType? = null,
        body: KType? = null,
        pathParams: KType? = null
    ) {
        openApi.addToPath(
            path,
            OpenAPI.Method.valueOf(method.value),
            response?.openApiKType?.let(responseBuilder) ?: emptyMap(),
            body?.openApiKType?.let(typeBuilder),
            pathParams?.openApiKType?.let(typeBuilder)
        )
    }

    fun addToPath(
        path: String,
        method: OpenAPI.Method,
        responses: Map<Int, Type> = emptyMap(),
        body: Type.Object? = null,
        pathParams: Type.Object? = null
    ) {
        openApi.addToPath(
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
                val response = Json {
                    encodeDefaults = false
                }
                    .encodeToString(openApi.root)
                context.call.response.status(HttpStatusCode.OK)

                context.call.respondText(response, contentType = ContentType.Application.Json)
                context.finish()
            } catch (ex: Exception) {
                logger.error(ex.message)
            }
        }
    }

    /**
     * Installable feature for [OpenApi].
     */
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, OpenApi> {
        override val key = AttributeKey<OpenApi>("OpenApi")
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): OpenApi {
            val configuration = Configuration().apply(configure)
            val feature = OpenApi(configuration)
            configuration.configure(feature.openApi)
            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }
            return feature
        }
    }
}
