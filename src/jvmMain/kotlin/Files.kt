package rtmigo.linecompress

import java.io.*
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.path.name


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

    fun dirtyn(n: Int): File = this.fileBase.parentFile.resolve(
            strippedName + "_" + n.toString() + DIRTY_ARCHIVE_SUFFIX
    )

    val compressed: File
        get() = this.fileBase.parentFile.resolve(
                strippedName + ARCHIVE_SUFFIX
        )

}

class LinesFile(private val file: File) {

    internal val triple = TripleName(file)

    fun append(line: String) {
        FileOutputStream(this.file, true).bufferedWriter().use { writer ->
            writer.appendLine(line)
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
    val lines: List<String>
        get() {
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

    private fun getDirtyFile(): File {
        for (i in 1..20) {
            val dirtyFile = this.triple.dirtyn(i)
            if (dirtyFile.createNewFile()) {
                return dirtyFile
            }

        }
        throw Exception("Failed to create new dirty file")
    }

    fun compress() {
        val dirtyFile = getDirtyFile()

        val data: ByteArray?

        try {
            data = this.triple.raw.readBytes()
        } catch (e: IOException) {
            // кто-то уже удалил оригинал данных. Возможно, параллельный поток
            return
        }

        FileOutputStream(dirtyFile).use { fileOut ->
            GZIPOutputStream(fileOut).use { zipOut ->
                zipOut.write(data)
            }
        }

        try {
            dirtyFile.renameTo(this.triple.compressed)
            this.triple.raw.delete()
        } catch (e: IOException) {
            // это стремный непротестированный момент (если переименовать или удалить
            // не получается). Полагаю, такое может произойти только из-за параллельных потоков,
            // и полагаю, вылетит именно IOException
            return
        }
    }
}