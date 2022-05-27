package dev.tmsoft.lib.upload.aws

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.endpoint.AwsEndpoint
import aws.sdk.kotlin.runtime.endpoint.AwsEndpointResolver
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.smithy.kotlin.runtime.http.Protocol
import aws.smithy.kotlin.runtime.http.Url
import aws.smithy.kotlin.runtime.http.operation.Endpoint
import dev.tmsoft.lib.upload.File
import dev.tmsoft.lib.upload.FileManager
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

    override suspend fun add(uploadFile: File, bucket: String, fileName: String?): Path = withContext(Dispatchers.IO) {
        s3.ensureBucketExists(bucket)
        s3.uploadImageToS3(uploadFile, bucket, ObjectCannedAcl.PublicRead, fileName)
    }

    override fun getWebUri(path: Path): String {
        val awsEndpoint = runBlocking { s3.config.endpointResolver.resolve(s3.serviceName, s3.config.region) }
        return if (path.isNotEmpty()) "${awsEndpoint.endpoint.uri}/$path".lowercase() else ""
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

internal class CustomEndpointResolver(private val hostname: String, private val protocol: String) :
    AwsEndpointResolver {
    override suspend fun resolve(service: String, region: String): AwsEndpoint {
        val uri = Url(scheme = Protocol.parse(protocol), host = hostname)
        return AwsEndpoint(Endpoint(uri = uri, isHostnameImmutable = true))
    }
}
