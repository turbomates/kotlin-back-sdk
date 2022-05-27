package dev.tmsoft.lib.upload.aws

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.smithy.kotlin.runtime.content.ByteStream
import dev.tmsoft.lib.upload.File
import dev.tmsoft.lib.upload.MimeType
import dev.tmsoft.lib.upload.Path
import java.util.UUID
import javax.activation.UnsupportedDataTypeException

data class AWS(
    val privateKey: String,
    val secret: String,
    val hostname: String?,
    val protocol: HttpProtocol = HttpProtocol.HTTPS
)

enum class HttpProtocol {
    HTTP,
    HTTPS
}

suspend fun S3Client.ensureBucketExists(bucketName: String) {
    if (!bucketExists(bucketName)) {
        createBucket {
            bucket = bucketName
            createBucketConfiguration {
                locationConstraint = BucketLocationConstraint.UsEast2
            }
        }
    }
}

suspend fun S3Client.bucketExists(s3bucket: String) =
    try {
        headBucket { bucket = s3bucket }
        true
    } catch (ignore: Exception) { // Checking Service Exception coming in future release
        false
    }

suspend fun S3Client.uploadImageToS3(file: File, bucket: String, acl: ObjectCannedAcl, fileName: String?): Path {
    val name = fileName ?: UUID.randomUUID().toString()
    when (file.mimeType) {
        MimeType.IMAGE -> putObject {
            this.bucket = bucket
            key = name
            body = ByteStream.fromBytes(file.content.readAllBytes())
            contentType = "image/jpeg"
            this.acl = acl
        }
        else -> throw UnsupportedDataTypeException("Unsupported Date Type for S3")
    }
    return "$bucket/$name"
}

// TODO("Not implemented")
// suspend fun S3Client.getPresignedUrl(bucket: String, fileName: String): Path {
//
//     val request =  GetObjectRequest {
//         key = fileName
//         this.bucket= bucket
//     }.presign(config, Duration.ofDays(1).toSeconds())
//     val resp = getObject(request)
//     return resp.url
// }
