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

internal data class DecisionBeforeAppending(val root: Path,
                                            val subdirs: Int,
                                            val last: Path?,
                                            val lastSize: Long,
                                            val bufferSize: Long
) {
    var rawToAppend: Path? = null
        private set

    var fileToRemove: Path? = null
        private set


    /** Compression source file or NULL if there is nothing to compress */
    var compressSource: Path? = null
        private set

    /** Compression target file or NULL if there is nothing to compress */
    var compressTarget: Path? = null
        private set

    override fun toString(): String {
        return "last: $last (size: $lastSize)\n" +
            "appendTo: $rawToAppend\n" +
            "remove: $fileToRemove\n" +
            "compressSrc: $compressSource\n" +
            "compressDst: $compressTarget"
    }

    init {
        if (last == null) {
            // file does not exist
            rawToAppend = NumberedFilePath.first(root, this.subdirs, RAW_SUFFIX).path
        }
        else {
            if (last.name.endsWith(RAW_SUFFIX)) {
                require(lastSize >= 0)
                require(bufferSize > 0)
                if (lastSize < bufferSize) {
                    rawToAppend = last
                }
                else {
                    compressSource = last
                    compressTarget = TripleName(last).compressed
                    rawToAppend = NumberedFilePath.fromPath(last, subdirs = subdirs).next.path
                }
            }
            else if (last.name.endsWith(ARCHIVE_SUFFIX)) {
                rawToAppend = NumberedFilePath.fromPath(
                    TripleName(last).raw,
                    subdirs = subdirs
                ).next.path
            } else if (last.name.endsWith(DIRTY_ARCHIVE_SUFFIX)) {
                fileToRemove = last
                assert(rawToAppend==null) // this means take one more step back in files tree
            } else {
                error("Unexpected file name: $last")
            }

        }
    }
}


class LinesDir(val path: Path, val subdirs: Int = 2, val bufferSize: Long = MEGABYTE) {

    private fun recurseFiles(reverse: Boolean) = recursePaths(path, reverse, subdirs)

    internal fun iterNumericThenNull(): Sequence<Path?> =
        recurseFiles(reverse = true) + sequenceOf(null)


    /** Если файл с максимальным числовым именем не особо большой, возвращаем его. Иначе
     * возвращаем новое имя файла.
     */
    internal fun fileForAppending(): Path {
        for (last in iterNumericThenNull()) {

            val decision = DecisionBeforeAppending(
                root = this.path,
                subdirs = this.subdirs,
                last = last,
                lastSize = last?.fileSize() ?: -1,
                bufferSize = this.bufferSize
            )

            if (decision.fileToRemove!=null) {
                decision.fileToRemove!!.toFile().delete()
            }

            if (decision.compressSource != null) {
                // TODO parallel thread?
                LinesFile(decision.compressSource!!).compress(
                    targetPath = decision.compressTarget!!
                )
            }

            if (decision.rawToAppend!=null)
                return decision.rawToAppend!!
        }

        error("Not expected to run thin line")
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