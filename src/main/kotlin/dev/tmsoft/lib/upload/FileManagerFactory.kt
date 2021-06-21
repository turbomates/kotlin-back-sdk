package dev.tmsoft.lib.upload

import com.google.inject.Inject
import dev.tmsoft.lib.upload.aws.AWS
import dev.tmsoft.lib.upload.aws.PublicS3Client

class FileManagerFactory @Inject constructor(private val configuration: AWS?, private val domain: String) {
    fun current(): FileManager {
        return configuration?.let { PublicS3Client(it) } ?: LocalFileManager(domain)
    }
}
