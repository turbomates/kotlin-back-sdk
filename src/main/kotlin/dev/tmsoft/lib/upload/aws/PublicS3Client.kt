package dev.tmsoft.lib.upload.aws

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.endpoints.EndpointParameters
import aws.sdk.kotlin.services.s3.endpoints.EndpointProvider
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.smithy.kotlin.runtime.client.endpoints.Endpoint
import aws.smithy.kotlin.runtime.net.Host
import aws.smithy.kotlin.runtime.net.Scheme
import aws.smithy.kotlin.runtime.net.Url
import dev.tmsoft.lib.upload.File
import dev.tmsoft.lib.upload.FileManager
import dev.tmsoft.lib.upload.Path
import dev.tmsoft.lib.upload.bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PublicS3Client constructor(private val config: AWS) : FileManager {
    private val s3: S3Client = S3Client {
        if (config.hostname != null) {
            endpointProvider = CustomEndpointProvider(config.hostname, config.protocol.toString())
        }
        region = config.region.value
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = config.privateKey
            secretAccessKey = config.secret
        }
    }

    override suspend fun add(uploadFile: File, bucket: String, fileName: String?): Path = withContext(Dispatchers.IO) {
        s3.ensureBucketExists(bucket, config.region)
        s3.uploadImageToS3(uploadFile, bucket, ObjectCannedAcl.PublicRead, fileName)
    }

    override fun getWebUri(path: Path): String {
        return if (path.isNotEmpty()) {
            val (bucket, url) = path.split("/", limit = 2)
            "https://$bucket.s3.amazonaws.com/$url"
        } else ""
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

internal class CustomEndpointProvider(private val hostname: String, private val protocol: String) : EndpointProvider {
    override suspend fun resolveEndpoint(params: EndpointParameters): Endpoint {
        val uri = Url(scheme = Scheme.parse(protocol), host = Host.parse(hostname))
        return Endpoint(uri)
    }
}
