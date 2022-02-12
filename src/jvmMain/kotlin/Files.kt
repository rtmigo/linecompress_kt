package rtmigo.linecompress

import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


internal const val ARCHIVE_SUFFIX = ".txt.gz"
internal const val ORIGINAL_SUFFIX = ".txt"
internal const val DIRTY_ARCHIVE_SUFFIX = ".txt.gz.tmp"

//fun isCompressedPath(file: Path) = file.name.endsWith(ARCHIVE_SUFFIX)

internal class TripleName(file: File) {

    private val fileBase = file

    internal val strippedName: String
        get() {
            for (e in listOf(ARCHIVE_SUFFIX, ORIGINAL_SUFFIX, DIRTY_ARCHIVE_SUFFIX)) {
                if (fileBase.name.endsWith(e)) {
                    return fileBase.name.dropLast(e.length)
                }
            }
            return fileBase.name
        }

    val raw: File
        get() = this.fileBase.parentFile.resolve(
                strippedName + ORIGINAL_SUFFIX
        )

    val dirty: File
        get() = this.fileBase.parentFile.resolve(
                strippedName + DIRTY_ARCHIVE_SUFFIX
        )

    val compressed: File
        get() = this.fileBase.parentFile.resolve(
                strippedName + ARCHIVE_SUFFIX
        )

}

class LinesFile(private val file: File) {

    internal val triple = TripleName(file)

    fun add(line: String) {
        if (line.contains('\n')) {
            throw IllegalArgumentException("Argument must not contain newline.")
        }
        // buffered write is thread-safe (https://stackoverflow.com/a/30275210)
        FileOutputStream(this.file, true).bufferedWriter().use {
            it.appendLine(line)
        }
    }

    val isCompressed: Boolean get() = this.triple.compressed.exists()

    val size: Long
        get() {
            if (this.triple.raw.exists()) {
                return this.triple.raw.length()
            }
            if (this.triple.compressed.exists()) {
                return this.triple.compressed.length()
            }
            return 0
        }

    /** Возвращает текстовые строки из файла. Неважно, они там еще в виде текста или уже сжаты. */
    fun readLines(): List<String>
       {
            if (this.isCompressed) {
                FileInputStream(this.triple.compressed).use { fileIn ->
                    GZIPInputStream(fileIn).use { zipIn ->
                        return zipIn.readBytes().decodeToString().lines().dropLast(1)
                    }
                }
            }
            else {
                return try {
                    this.triple.raw.readLines()
                } catch (_: FileNotFoundException) {
                    listOf()
                }
            }
        }

    fun compress() {
        FileOutputStream(triple.dirty).use { fileOut ->
            fileOut.channel.use {
                if (triple.compressed.exists()) {
                    // похоже, другой поток его уже сжал
                    return
                }
                GZIPOutputStream(fileOut).use { zipOut ->
                    zipOut.write(this.triple.raw.readBytes())
                }
                triple.dirty.renameTo(this.triple.compressed)
                this.triple.raw.delete()
            }
        }
    }
}