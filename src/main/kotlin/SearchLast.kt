/**
 * SPDX-FileCopyrightText: (c) 2022 Artёm IG <github.com/rtmigo>
 * SPDX-License-Identifier: MIT
 *
 */

/**
 * Функции, помогающие в дереве каталогов найти файл вроде 123/456/789.zip,
 * у которого вот это число 123456789 максимально.

 * Мы стремимся НЕ обходить рекурсивно все дерево каталогов. Мы просто берем
 * последний подкаталог из '123/', потом последний файл из '123/456/'.

 * Возможен подвох: например, у нас есть пустой каталог '999/' и пустой '999/888'.
 * Их нужно проигнорировать и добраться до '123/456/789.zip' кратчайшим путем.

 * Среди файлов и каталогов нас интересуют только те, имена которых начинаются
 * с чисел. Например, '555', '555suffix', '555.zip'.
 */

package io.github.rtmigo.linecompress

import java.nio.file.NotDirectoryException
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

internal fun stringsSortedByNumPrefix(namesSameDir: List<String>, reverse: Boolean): List<String> {
    return namesSameDir
            .map { Pair(numPrefix(it), it) }
            .filter { it.first != null }
            .sortedBy { if (reverse) -it.first!! else it.first!! }
            .map { it.second }
}

internal fun directoryEntriesSortedByNumPrefix(parent: Path, reverse: Boolean) = sequence<Path> {
    try {
        for (basename in stringsSortedByNumPrefix(
                parent.listDirectoryEntries().map { it.name },
                reverse
        )) {
            yield(parent.resolve(basename))
        }
    } catch (_: NotDirectoryException) {
        // happens when we try to list a file
    }
}

/**
 * Обходим дерево каталогов.
 *
 * Все результаты будут отсортированы по значениям числовых префиксов:
 *
 * * 100a/200b/998c
 * * 100a/200b/999c
 * * 100a/201b/000c
 *
 * Если числового префикса нет - путь (файл или каталог) игнорируется.
 *
 * Пустые каталоги игнорируются.
 *
 * Короткие пути, вроде '100a/200b', если мы ищем путь из трех частей -
 * игнорируются.
 *
 */
internal fun recursePaths(parent: Path, reverse: Boolean, goDeeper: Int): Sequence<Path> =
    sequence {
        if (goDeeper == 0) {
            for (result in directoryEntriesSortedByNumPrefix(parent, reverse)) {
                yield(result)
            }
        }
        else {
            for (sub in directoryEntriesSortedByNumPrefix(parent, reverse)) {
                for (result in recursePaths(sub, reverse, goDeeper - 1)) {
                    yield(result)
                }
            }
        }
    }
