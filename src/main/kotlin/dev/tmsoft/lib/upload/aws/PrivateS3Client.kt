package dev.tmsoft.lib.upload.aws

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import dev.tmsoft.lib.upload.File
import dev.tmsoft.lib.upload.FileManager
import dev.tmsoft.lib.upload.Path
import dev.tmsoft.lib.upload.bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivateS3Client constructor(private val config: AWS) : FileManager {
    private val s3: S3Client = S3Client {
        region = config.region.value
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = config.privateKey
            secretAccessKey = config.secret
        }
    }

    override suspend fun add(uploadFile: File, bucket: String, fileName: String?): Path = withContext(Dispatchers.IO) {
        s3.ensureBucketExists(bucket, config.region)
        s3.uploadImageToS3(uploadFile, bucket, ObjectCannedAcl.Private, fileName)
    }

    override fun getWebUri(path: Path): String {
        TODO("Not implemented")
        // https://docs.aws.amazon.com/AmazonS3/latest/userguide/ShareObjectPreSignedURL.html#awsui-expandable-section-2-trigger
        // return s3.getPresignedUrl()
    }

    override suspend fun remove(path: String) {
        val bucket = path.bucket
        s3.deleteObject(
            DeleteObjectRequest {
                this.bucket = bucket
                key = path
            }
        )
    }
}
