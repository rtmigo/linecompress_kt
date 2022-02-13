/**
 * SPDX-FileCopyrightText: (c) 2022 Artёm IG <github.com/rtmigo>
 * SPDX-License-Identifier: MIT
 *
 */

import io.github.rtmigo.linecompress.LinesFile
import io.github.rtmigo.linecompress.TripleName
import org.junit.After
import org.junit.Before
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


internal class LinesFileTest {
    var tempDir: File? = null

    @Before
    fun setupTest() {
        tempDir = Files.createTempDirectory("LinesFileTest").toFile()
    }

    @After
    fun teardownTest() {
        tempDir!!.deleteRecursively()
    }


    @Test
    fun testNameWithoutExt() {
        assertEquals(
                "something",
                TripleName(File("/path/to/something.txt.gz")).strippedName
        )
        assertEquals(
                "something",
                TripleName(File("/path/to/something.txt")).strippedName
        )
        assertEquals(
                "something",
                TripleName(File("/path/to/something.txt.gz.tmp")).strippedName
        )
        assertEquals(
                "something.jpg",
                TripleName(File("/path/to/something.jpg")).strippedName
        )
    }

    @Test
    fun testNameAs() {
        val tn = TripleName(File("/path/to/something.txt.gz"))

        assertEquals(
                File("/path/to/something.txt"), tn.raw
        )

        assertEquals(
                File("/path/to/something.txt.gz"), tn.compressed
        )

        assertEquals(
            File("/path/to/something.txt.gz.tmp"), tn.dirty
        )
    }

    @Test
    fun testCannotAddNewline() {
        val lf = LinesFile(tempDir!!.resolve("child.txt"))
        assertThrows<IllegalArgumentException> { lf.add("abc\ndef") }

    }


    @Test
    fun testWrite() {
        val lf = LinesFile(tempDir!!.resolve("child.txt"))
        lf.add("Line 1")
        lf.add("Line 2")
        assertEquals(lf.readLines(), listOf("Line 1", "Line 2"))
        lf.add("Line 3")
        assertEquals(lf.readLines(), listOf("Line 1", "Line 2", "Line 3"))
    }

    @Test
    fun testReadEmpty() {
        val lf = LinesFile(tempDir!!.resolve("child.txt"))
        assertEquals(lf.readLines(), listOf())
    }

    @Test
    fun testCompressAndRead() {
        val lf = LinesFile(tempDir!!.resolve("child.txt"))

        lf.add("Line 1")
        lf.add("Line 2")
        lf.add("Line 3")

        assert(!lf.isCompressed)

        lf.compress()

        assert(lf.isCompressed)
        assert(!lf.triple.raw.exists())

        assertEquals(listOf("Line 1", "Line 2", "Line 3"), lf.readLines())
    }

    @Test
    fun testDancing() {
        val fileText: String = ClassLoader.getSystemResource("dancing.txt").readText()

        val originalLines = fileText.lines()
        val lf = LinesFile(tempDir!!.resolve("compressed.txt"))
        assertEquals(0, lf.size)
        for (line in originalLines) {
            lf.add(line)
        }
        assertEquals(57316, lf.size)

        lf.compress()

        assertTrue(lf.size > 1000)
        assertTrue(lf.size < 40000)

        assertEquals(lf.readLines(), originalLines)
    }
}