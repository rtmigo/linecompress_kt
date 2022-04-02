/**
 * SPDX-FileCopyrightText: (c) 2022 Artёm IG <github.com/rtmigo>
 * SPDX-License-Identifier: MIT
 *
 */


package io.github.rtmigo.linecompress

import java.io.*
import java.nio.file.Path
import java.util.zip.*
import kotlin.io.path.*


internal const val ARCHIVE_SUFFIX = ".txt.gz"
internal const val RAW_SUFFIX = ".txt"
internal const val DIRTY_ARCHIVE_SUFFIX = ".txt.gz.tmp"

internal class TripleName(private val source: Path) {

    internal val strippedName: String
        get() {
            for (e in listOf(ARCHIVE_SUFFIX, RAW_SUFFIX, DIRTY_ARCHIVE_SUFFIX)) {
                if (source.name.endsWith(e)) {
                    return source.name.dropLast(e.length)
                }
            }
            return source.name
        }

    val raw: Path
        get() = this.source.parent.resolve(
            strippedName + RAW_SUFFIX
        )

    val dirty: Path
        get() = this.source.parent.resolve(
            strippedName + DIRTY_ARCHIVE_SUFFIX
        )

    val compressed: Path
        get() = this.source.parent.resolve(
            strippedName + ARCHIVE_SUFFIX
        )
}

class LinesFile(private val file: Path) {

    internal val triple = TripleName(file)

    fun add(line: String) {
        if (line.contains('\n')) {
            throw IllegalArgumentException("Argument must not contain newline.")
        }
        // buffered write is thread-safe (https://stackoverflow.com/a/30275210)
        FileOutputStream(this.file.toFile(), true).bufferedWriter().use {
            it.appendLine(line)
        }
    }

    val isCompressed: Boolean get() = this.triple.compressed.exists()

    val size: Long
        get() {
            if (this.triple.raw.exists()) {
                return this.triple.raw.fileSize()
            }
            if (this.triple.compressed.exists()) {
                return this.triple.compressed.fileSize()
            }
            return 0
        }

    /** Возвращает текстовые строки из файла. Неважно, они там еще в виде текста или уже сжаты. */
    fun readLines(): List<String> {
        if (this.isCompressed) {
            FileInputStream(this.triple.compressed.toFile()).use { fileIn ->
                GZIPInputStream(fileIn).use { zipIn ->
                    return zipIn.readBytes().decodeToString().lines().dropLast(1)
                }
            }
        }
        else {
            return try {
                this.triple.raw.readLines()
            } catch (_: kotlin.io.NoSuchFileException) {
                listOf()
            } catch (_: java.nio.file.NoSuchFileException) {
                listOf()
            }

        }
    }

    internal fun compress(targetPath: Path = this.triple.compressed) {
        FileOutputStream(triple.dirty.toFile()).use { fileOut ->
            fileOut.channel.use {
                if (targetPath.toFile().exists()) {
                    // похоже, другой поток его уже сжал
                    return
                }
                MaxCompressionGzipStream(fileOut).use { zipOut ->
                    zipOut.write(this.triple.raw.readBytes())
                }
                triple.dirty.moveTo(targetPath)
                this.triple.raw.deleteExisting()
            }
        }
    }
}

private class MaxCompressionGzipStream(out: OutputStream?) :
    GZIPOutputStream(out) {
        init {
            def.setLevel(9)
        }
}