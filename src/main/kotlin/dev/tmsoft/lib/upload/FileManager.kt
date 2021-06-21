package dev.tmsoft.lib.upload

typealias Path = String

interface FileManager {
    suspend fun add(image: Image, bucket: String, fileName: String? = null): Path
    fun getWebUri(path: Path): String
    suspend fun remove(path: String)
}
