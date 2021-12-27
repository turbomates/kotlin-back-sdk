package dev.tmsoft.lib.upload.aws

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import dev.tmsoft.lib.upload.FileManager
import dev.tmsoft.lib.upload.Image
import dev.tmsoft.lib.upload.Path
import dev.tmsoft.lib.upload.bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PrivateS3Client constructor(private val config: AWS) : FileManager {
    private val s3: S3Client = S3Client {
        region = BucketLocationConstraint.UsEast2.value
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = config.privateKey
            secretAccessKey = config.secret
        }
    }

    override suspend fun add(image: Image, bucket: String, fileName: String?): Path = withContext(Dispatchers.IO) {
        s3.ensureBucketExists(bucket)
        s3.close()
        s3.uploadImageToS3(image, bucket, ObjectCannedAcl.Private, fileName)
    }

    override suspend fun getWebUri(path: Path): String {
        TODO("Not implemented")
        //https://stackoverflow.com/questions/34993366/how-to-get-public-url-after-uploading-image-to-s3
        //https://stackoverflow.com/questions/10663238/how-to-create-download-link-for-an-amazon-s3-buckets-object
        //return s3.utilities().getUrl { it.bucket(config.bucket).key(path) }.toExternalForm()
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
