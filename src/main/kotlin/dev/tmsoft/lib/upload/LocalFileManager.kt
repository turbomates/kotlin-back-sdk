package dev.tmsoft.lib.upload

import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class LocalFileManager(private val domain: String) : FileManager {
    override suspend fun add(
        image: Image,
        bucket: String,
        fileName: String?
    ): Path = withContext(Dispatchers.IO) {
        val name = fileName ?: UUID.randomUUID().toString()
        val dir = File("$BASE_UPLOADS_DIR/$bucket")
        if (!dir.exists()) dir.mkdirs()

        val fullFileName = "$name.${image.extension}".lowercase()
        val path = "$bucket/$fullFileName".lowercase()
        File("$BASE_UPLOADS_DIR/$path").writeBytes(image.content.readAllBytes())

        path
    }

    override fun getWebUri(path: Path): String {
        return if (path.isNotEmpty()) "$domain/$path".lowercase() else ""
    }

    override suspend fun remove(path: String) {
        val file = get(path)
        file.delete()
    }

    private fun get(path: Path): File {
        val file = File("$BASE_UPLOADS_DIR/$path".lowercase())
        if (!file.exists()) throw NoSuchElementException("File not found")

        return file
    }

    companion object {
        private const val BASE_UPLOADS_DIR = "uploads"
    }
}
