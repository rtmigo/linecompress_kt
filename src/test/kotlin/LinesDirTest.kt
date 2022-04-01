/**
 * SPDX-FileCopyrightText: (c) 2022 Artёm IG <github.com/rtmigo>
 * SPDX-License-Identifier: MIT
 *
 */

import io.github.rtmigo.linecompress.*
import org.junit.jupiter.api.assertThrows
import java.nio.file.*
import kotlin.io.path.*
import kotlin.random.Random
import kotlin.test.*


class SplitNumsTest {
    @Test
    fun testSplitNums() {
        assert(splitNums(123456789) == listOf(123, 456, 789))
        assert(splitNums(12304560789) == listOf(12, 304, 560, 789))
    }

    @Test
    fun testSplitNumsLength() {
        assert(splitNums(123, length = 1) == listOf(123))
        assert(splitNums(123, length = 2) == listOf(0, 123))
        assert(splitNums(123, length = 3) == listOf(0, 0, 123))
    }
}

class CombineNumsTest {
    @Test
    fun testIntPow() {
        assert(intPow(3, 0) == 1L)
        assert(intPow(3, 1) == 3L)
        assert(intPow(3, 2) == 9L)
        assert(intPow(3, 3) == 27L)
    }


    @Test
    fun testCombine() {
        assert(combineNums(listOf(123, 456)) == 123456L)
        assert(combineNums(listOf(1, 234, 567)) == 1234567L)
        assert(combineNums(listOf(1)) == 1L)
    }
}

class NumberedFilePathTest {
    @Test
    fun testOne() {
        assertEquals(
            Path("/path/to/005/023"),
            NumberedFilePath(Path("/path/to"), listOf(5, 23)).path
        )
    }

    @Test
    fun testFirst() {
        assertEquals(
            Path("/path/to/000/000/000"),
            NumberedFilePath.first(Path("/path/to")).path
        )
        assertEquals(
            Path("/path/to/000/000/000/000/000/000"),
            NumberedFilePath.first(Path("/path/to"), subdirs = 5).path
        )
        assertEquals(
            Path("/path/to/000"),
            NumberedFilePath.first(Path("/path/to"), subdirs = 0).path
        )
    }

    @Test
    fun testFirstWithSuffix() {
        assertEquals(
            Path("/path/to/000/000/000.zip"),
            NumberedFilePath.first(Path("/path/to"), suffix = ".zip").path
        )
    }

    @Test
    fun testFirstEmpty() {
        assertEquals(
            Path("000/000/000"),
            NumberedFilePath.first(Path("")).path
        )
    }

    @Test
    fun testNumPrefixStr() {
        assertEquals(
            "321",
            numPrefixStr("321abc")
        )
        assertEquals(
            "5",
            numPrefixStr("5")
        )
        assertEquals(
            null,
            numPrefixStr("x5")
        )
    }

    @Test
    fun testFromPath() {
        val p = Path("/path/to/012/345/456")
        assertEquals(
            p,
            NumberedFilePath.fromPath(p, subdirs = 2).path
        )
    }

    @Test
    fun testNext() {
        assertEquals(
            Path("/a/b/c/000/000/001.xz"),
            NumberedFilePath.fromPath(Path("/a/b/c/000/000/000.xz")).next.path
        )

        assertEquals(
            Path("/a/b/c/000/000/999.xz"),
            NumberedFilePath.fromPath(Path("/a/b/c/000/000/998.xz")).next.path
        )

        assertEquals(
            Path("/a/b/c/000/001/000.xz"),
            NumberedFilePath.fromPath(Path("/a/b/c/000/000/999.xz")).next.path
        )

        assertEquals(
            Path("/a/b/c/001/000/000.xz"),
            NumberedFilePath.fromPath(Path("/a/b/c/000/999/999.xz")).next.path
        )
    }

    @Test
    fun testNextTooLarge() {
        // нет проблем
        NumberedFilePath.fromPath(Path("/a/b/c/999/999/998.xz")).next

        // есть проблемы
        assertThrows<IllegalArgumentException> {
            NumberedFilePath.fromPath(Path("/a/b/c/999/999/999.xz")).next
        }

    }

    @Test
    fun testParsePath() {
        for (p in listOf(
            Path("/path/to/123/456/789"),
            Path("/path/to/123/456/789.zip"),
            Path("/path/to/123/456/789file.xz"),
            Path("123/456/789"),
        )) {
            assertEquals(
                p,
                NumberedFilePath.fromPath(p).path
            )
        }
    }
}

class SearchLastTest {
    @Test
    fun testSortedByNumPrefix() {
        assertEquals(
            listOf("1def", "02pepe", "05abc", "88zys"),
            stringsSortedByNumPrefix(
                listOf("05abc", "1def", "88zys", "02pepe"),
                reverse = false
            )
        )

        assertEquals(
            listOf("1def", "02pepe", "05abc", "88zys").reversed(),
            stringsSortedByNumPrefix(listOf("05abc", "1def", "88zys", "02pepe"), reverse = true)
        )
    }
}

/** Проверяем, как `LinesDir` обнаруживает последний (по номеру) файл. Это файл, к которому
 * мы обычно добавляем новые строки. */
internal class FindLastFileTest {

    private fun createAndCompare(
        filesToCreate: List<String>,
        expectedLast: String?
    ) {
        val tempDir = Files.createTempDirectory("FindLastFileTest").toAbsolutePath()

        try {
            val root = tempDir
            for (f in filesToCreate.shuffled()) {
                val fullpath = root.resolve(f)

                fullpath.parent.createDirectories()

                assert(!fullpath.exists())
                fullpath.createFile()
                assert(fullpath.exists())
            }

            if (expectedLast == null) {
                assertEquals(null, LinesDir(root).numericallyLastRawFile())
            }
            else {
                assertEquals(
                    root.resolve(expectedLast),
                    LinesDir(root).numericallyLastRawFile()
                )
            }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testLastFileSimple() {
        for (lst in listOf(
            listOf(
                "000/000/000.txt",
                "000/000/001.txt",
                "000/000/002.txt",
            ),
            listOf(
                "000/000/888.txt",
                "000/000/889.txt",
                "000/002/001.txt",
            ),
            listOf(
                "000/000/888.txt",
                "000/000/889.txt",
                "000/002/001.txt",
                "005/002/001.txt",
            )
        )) {
            createAndCompare(lst, expectedLast = lst.last())
        }
    }

    @Test
    fun testLastFileSkipEmptyDirs() {
        createAndCompare(
            listOf(
                "000/000/888.txt",
                "000/000/889.txt",
                "000/002/001.txt",
                "005/002/001.txt",
                "006/098/",
                "006/099/",
                "026/",
                "076/099/",
            ),
            expectedLast = "005/002/001.txt"
        )
    }

    @Test
    fun testEmpty() {
        createAndCompare(
            listOf(
                "000/000",
                "000/001",
                "002/001",
                "005/005/abc",
                "005/005/def",
            ),
            expectedLast = null
        )
    }
}


/** Добавляем данные в каталог и проверяем, что "файл для добавления" меняется
 * на следующий, как только данных добавлено определенное количество. */
class TestFillDir {

    private fun randomString(length: Int): String {
        val chars = "0123456789".toCharArray()
        return (1..length)
            .map { chars[Random.nextInt(0, chars.size)] }
            .joinToString("")
    }

    @Test
    fun testFillDir() {
        val tempParentDir = Files.createTempDirectory("FindLastFileTest").toAbsolutePath()
        try {

            val ld = LinesDir(tempParentDir, bufferSize = 150)

            fun addRandomLine() = ld.add(randomString(50))

            fun expect(fn: String) = assertEquals(tempParentDir.resolve(fn), ld.fileForAppending())

            expect("000/000/000.txt")
            addRandomLine()
            expect("000/000/000.txt")
            addRandomLine()
            expect("000/000/000.txt")

            // размер файла уже максимальный - и следующий раз мы будем
            // добавлять к другому файлу
            addRandomLine()
            expect("000/000/001.txt")

        } finally {
            tempParentDir.toFile().deleteRecursively()
        }
    }
}

class TestReadWriteDir {

    @Test
    fun testReadWriteSingleLine() {
        val tempParentDir = Files.createTempDirectory("TestReadWriteDir").toAbsolutePath()
        try {
            val ld = LinesDir(tempParentDir)
            ld.add("hi!")
            assertEquals(listOf("hi!"), ld.readLines().toList())
        } finally {
            tempParentDir.toFile().deleteRecursively()
        }
    }
}

val holmesText by lazy {
    ClassLoader.getSystemResource("dancing.txt").readText()
}

internal class HolmesDirTest {
    var testDir: Path? = null

    companion object {
        // variables you initialize for the class just once:
        private val fileText: String = holmesText
        val originalLines = fileText.lines()

        init {
            assert(originalLines.size == 1131) { originalLines.size.toString() }
        }
    }

    @BeforeTest
    fun setup() {
        //println("Setup")
        testDir = Files.createTempDirectory("FindLastFileTest").toAbsolutePath()
        val ld = LinesDir(testDir!!, bufferSize = 1024)
        for (line in originalLines) {
            ld.add(line)
        }
    }

    @AfterTest
    fun teardown() {
        testDir!!.toFile().deleteRecursively()
    }

    @Test
    fun weHaveManyFiles() {
        assertEquals(
            testDir!!.resolve("000/000/054.txt"),
            LinesDir(testDir!!).fileForAppending()
        )
    }

    @Test
    fun readForward() {
        assertContentEquals(
            originalLines,
            LinesDir(testDir!!).readLines().toList()
        )
    }

    @Test
    fun readReverse() {
        assertContentEquals(
            originalLines.reversed(),
            LinesDir(testDir!!).readLines(reverse = true).toList()
        )
    }
}

open class WithTestDir {
    lateinit var testDir: Path

    @BeforeTest
    fun setup() {
        //println("Setup")
        testDir = Files.createTempDirectory("SubdirsTest").toAbsolutePath()

    }

    @AfterTest
    fun teardown() {
        testDir.toFile().deleteRecursively()
    }

    fun filesCount(): Int = testDir.toFile().walk().count()

    fun filesCountWithSuffix(suf: String)
        = testDir.toFile().walk().count() { it.toString().endsWith(suf) }
}

class SubdirsTest : WithTestDir() {



    fun runTest(subdirs: Int) {
        assertEquals(1, filesCount())
        val ld = LinesDir(testDir, bufferSize = 1024, subdirs = subdirs)
        holmesText.lines().forEach { ld.add(it) }
        assertTrue(filesCount() > 50)
        assertContentEquals(ld.readLines().toList(), holmesText.lines())
    }

    @Test
    fun `1 subdir`() {
        runTest(1)
    }

    @Test
    fun `2 subdirs`() {
        runTest(2)
    }

    @Test
    fun `3 subdirs`() {
        runTest(3)
    }
}

class SubdirsAfterArchiveCreation : WithTestDir() {
    // если мы создали архив GZIP и сразу перезапустили приложение - т.е. файла-буфера
    // у нас просто нет

    @Test
    fun tst() {

        fun createLinesDir() = LinesDir(testDir, bufferSize = 100)

        val a = createLinesDir()
        a.add("something")
        val initialFilesCount = filesCount()

        do {
            a.add("Twas brillig, and the slithy toves did gyre and gimble in the wabe.")
        } while (filesCount()==initialFilesCount)

        assertEquals(1, filesCountWithSuffix(ARCHIVE_SUFFIX))
        assertEquals(1, filesCountWithSuffix(RAW_SUFFIX))


        // удаляем новейший txt-файл
        a.fileForAppending().deleteExisting()
        assert(filesCount()==initialFilesCount)

        assertEquals(1, filesCountWithSuffix(ARCHIVE_SUFFIX))
        assertEquals(0, filesCountWithSuffix(RAW_SUFFIX))


        // создаём новый экземпляр объекта и продолжаем запись
        val b = createLinesDir()
        b.add("All mimsy were the borogoves, and the mome raths outgrabe. ")
        assertEquals(1, filesCountWithSuffix(ARCHIVE_SUFFIX), message = testDir.toFile().walk().toList().toString())
        assertEquals(1, filesCountWithSuffix(RAW_SUFFIX))
    }

}