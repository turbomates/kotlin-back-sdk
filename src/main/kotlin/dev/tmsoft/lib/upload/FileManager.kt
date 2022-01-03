package dev.tmsoft.lib.upload

typealias Path = String

internal val Path.bucket: String
    get() = split("/").first()


interface FileManager {
    suspend fun add(image: Image, bucket: String, fileName: String? = null): Path
    fun getWebUri(path: Path): String
    suspend fun remove(path: String)
}
