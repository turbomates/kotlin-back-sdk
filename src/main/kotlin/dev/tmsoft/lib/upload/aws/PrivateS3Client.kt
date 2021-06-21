package dev.tmsoft.lib.upload.aws

import dev.tmsoft.lib.upload.FileManager
import dev.tmsoft.lib.upload.Image
import dev.tmsoft.lib.upload.Path
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class PrivateS3Client constructor(private val config: AWS) : FileManager {
    private val credentials = AwsBasicCredentials.create(config.privateKey, config.secret)
    private val s3: S3Client = S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .region(Region.EU_CENTRAL_1)
        .build()

    override suspend fun add(image: Image, bucket: String, fileName: String?): Path = withContext(Dispatchers.IO) {
        val name = fileName ?: UUID.randomUUID().toString()
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(config.bucket)
                .acl(ObjectCannedACL.PRIVATE)
                .contentType("image/jpeg")
                .key(name)
                .build(),
            RequestBody.fromBytes(image.content.readAllBytes())
        )
        name
    }

    override fun getWebUri(path: Path): String {
        return s3.utilities().getUrl { it.bucket(config.bucket).key(path) }.toExternalForm()
    }

    override suspend fun remove(path: String) {
        s3.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(config.bucket)
                .key(path)
                .build()
        )
    }
}
