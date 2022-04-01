/**
 * SPDX-FileCopyrightText: (c) 2022 Artёm IG <github.com/rtmigo>
 * SPDX-License-Identifier: MIT
 *
 */

package io.github.rtmigo.linecompress

import java.nio.file.*
import kotlin.io.path.*


/** Делит большое число вроде 1234567 на группы по три цифры: [1, 234, 567] */
internal fun splitNums(x: Long, length: Int? = null): List<Int> {
    val result = mutableListOf<Int>()

    var i: Long = x
    while (i > 0) {
        result.add((i % 1000).toInt())
        i /= 1000
    }

    if (length != null) {
        if (result.size > length) {
            throw IllegalArgumentException(
                "Number $x splits to ${result.reversed()} which is longer than $length."
            )
        }
        while (result.size < length) {
            result.add(0)
        }
    }
    return result.reversed().toList()
}

/** Возводит число в степень (без конвертирования в Double и обратно) */
internal fun intPow(base: Int, exponent: Int): Long {
    var result: Long = 1
    var exp = exponent
    while (exp != 0) {
        result *= base
        --exp
    }
    return result
}

/** Набор цифр вроде [1, 234, 567] превращает в большое число вроде 1234567  */
internal fun combineNums(nums: List<Int>): Long {
    return nums.reversed().withIndex<Int>().sumOf { intPow(1000, it.index) * it.value }
}

internal fun numPrefixStr(text: String): String? {
    return Regex("""^\d+""").find(text)?.value
}

internal fun numPrefix(text: String): Int? = numPrefixStr(text)?.toInt()


internal class NumberedFilePath(val root: Path, val nums: List<Int>, val suffix: String = "") {

    init {
        for (n in nums) {
            if (n !in 0..999) {
                throw IllegalArgumentException()
            }
        }
    }

    companion object Factory {
        // * nums - это числа вроде [1, 234, 567], последнее из которых соответствует имени файла
        // * subdirs - это количество подкаталогов в корневом каталоге. В данном случае
        //   подкаталогами будут "001/234" - то есть, подкаталогов два, хотя чисел три

        fun first(root: Path, subdirs: Int = 2, suffix: String = ""): NumberedFilePath =
            NumberedFilePath(root, nums = List<Int>(subdirs + 1) { 0 }, suffix = suffix)

        fun fromPath(file: Path, subdirs: Int = 2): NumberedFilePath {
            val fileNum = numPrefixStr(file.name)
                ?: throw IllegalArgumentException(file.toString())
            val suffix = file.name.substring(fileNum.length)

            val nums = mutableListOf<Int>(fileNum.toInt())
            var p = file
            for (i in 1..subdirs) {
                p = p.parent
                nums.add(p.name.toInt())
            }
            nums.reverse()

            val root = p.parent
            val result = NumberedFilePath(
                root = if (p.parent != null) p.parent else Paths.get(""),
                nums = nums,
                suffix = suffix
            )
            assert(result.path == file)
            assert(result.nums.size == subdirs + 1)
            return result
        }
    }

    val path: Path
        get() {
            var result = root
            for (n in nums) {
                result = result.resolve(n.toString().padStart(3, '0'))
            }
            if (suffix.isNotEmpty()) {
                result = result.parent.resolve(result.name + suffix)
            }
            return result
        }

    val next: NumberedFilePath
        get() {
            return NumberedFilePath(
                this.root,
                splitNums(combineNums(this.nums) + 1, length = this.nums.size),
                suffix = suffix
            )
        }
}


const val MEGABYTE: Long = 1000 * 1000

//data class NextDecision(val last: Path?) {
//
//}

class LinesDir(val path: Path, val subdirs: Int = 2, val bufferSize: Long = MEGABYTE) {

    private fun recurseFiles(reverse: Boolean) = recursePaths(path, reverse, subdirs)

    internal fun numericallyLastRawFile(): Path? =
        recurseFiles(reverse = true)
            .filter { it.name.endsWith(RAW_SUFFIX) }
            .firstOrNull()

    /** Если файл с максимальным числовым именем не особо большой, возвращаем его. Иначе
     * возвращаем новое имя файла.
     */
    internal fun fileForAppending(): Path {
        val last = numericallyLastRawFile()
            ?: // file does not exist
            return NumberedFilePath.first(this.path, this.subdirs, RAW_SUFFIX).path

        val triple = TripleName(last.toFile())
        if (!triple.raw.exists()
            || compressedIt(triple.raw.toPath())
            || !weHaveOnlyRawFile(triple)
        ) {
            // we cannot append to last file, so we'll return a new
            // name (for a file that does not exist yet)
            return NumberedFilePath.fromPath(last, subdirs = subdirs).next.path
        }
        return last
    }

    private fun weHaveOnlyRawFile(file: TripleName): Boolean {
        return file.raw.exists() && !file.compressed.exists()
    }

    private fun compressedIt(file: Path): Boolean {
        if (file.fileSize() >= bufferSize) {
            assert(file.exists())
            LinesFile(file).compress()
            assert(!file.exists()) // raw file removed
            return true
        }
        return false
    }

    @Synchronized
    fun add(text: String) {
        val path = fileForAppending()
        path.parent.createDirectories()
        LinesFile(path).add(text)
    }

    /** Возвращает все строки из всех файлов. */
    fun readLines(reverse: Boolean = false): Sequence<String> = sequence {
        for (file in recurseFiles(reverse = reverse)) {
            val lf = LinesFile(file)
            val fileLines = if (reverse) lf.readLines().reversed() else lf.readLines()
            for (line in fileLines) {
                yield(line)
            }
        }
    }
}