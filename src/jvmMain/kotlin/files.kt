package rtmigo.linecompress

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.path.name


internal const val ARCHIVE_SUFFIX = ".txt.gz"
internal const val ORIGINAL_SUFFIX = ".txt"
internal const val DIRTY_ARCHIVE_SUFFIX = ".txt.gz.tmp"

fun isCompressedPath(file: Path) = file.name.endsWith(ARCHIVE_SUFFIX)

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
            } else {
                return try {
                    this.triple.raw.readLines()
                } catch (_: FileNotFoundException) {
                    listOf()
                }
            }
        }

    fun compress() {
        if (this.isCompressed) {
            throw Error("The file is already compressed")
        }

        val dirtyFile = this.triple.dirty
        dirtyFile.delete()  // этот вызов почему-то срабатывает даже если файла нет

        FileOutputStream(this.triple.dirty).use { fileOut ->
            GZIPOutputStream(fileOut).use { zipOut ->
                zipOut.write(this.triple.raw.readBytes())
            }
        }

        assert(!this.triple.compressed.exists())
        assert(this.triple.dirty.exists())
        assert(dirtyFile == this.triple.dirty)

        dirtyFile.renameTo(this.triple.compressed)
        this.triple.raw.delete()

        assert(this.isCompressed)
    }
}