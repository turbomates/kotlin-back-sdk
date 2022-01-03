package dev.tmsoft.lib.upload.aws

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.endpoint.Endpoint
import aws.sdk.kotlin.runtime.endpoint.EndpointResolver
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import dev.tmsoft.lib.upload.FileManager
import dev.tmsoft.lib.upload.Image
import dev.tmsoft.lib.upload.Path
import dev.tmsoft.lib.upload.bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class PublicS3Client constructor(private val config: AWS) : FileManager {
    private val s3: S3Client = S3Client {
        config.hostname?.let { endpointResolver = CustomEndpointResolver(config.hostname, config.protocol.toString()) }
        region = BucketLocationConstraint.UsEast2.value
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = config.privateKey
            secretAccessKey = config.secret
        }
    }

    override suspend fun add(image: Image, bucket: String, fileName: String?): Path = withContext(Dispatchers.IO) {
        s3.ensureBucketExists(bucket)
        s3.uploadImageToS3(image, bucket, ObjectCannedAcl.PublicRead, fileName)
    }

    override fun getWebUri(path: Path): String {
        val endpoint = runBlocking { s3.config.endpointResolver.resolve(s3.serviceName, s3.config.region) }
        return if (path.isNotEmpty()) "${endpoint.protocol}://${endpoint.hostname}/$path".lowercase() else ""
    }

    override suspend fun remove(path: Path) {
        val bucket = path.bucket
        s3.deleteObject(
            DeleteObjectRequest {
                this.bucket = bucket
                key = path
            }
        )
    }
}

internal class CustomEndpointResolver(val hostname: String, val protocol: String) : EndpointResolver {
    override suspend fun resolve(service: String, region: String): Endpoint {
        return Endpoint(hostname = hostname, protocol = protocol, isHostnameImmutable = true)
    }
}
