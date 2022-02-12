import rtmigo.linecompress.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// https://www.spekframework.org/
// https://github.com/kotest/kotest

internal class LinesFileTest {
    @TempDir
    @JvmField
    var tempDir: File? = null

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
    fun testWrite() {
        val lf = LinesFile(tempDir!!.resolve("child.txt"))
        lf.append("Line 1")
        lf.append("Line 2")
        assertEquals(lf.lines, listOf("Line 1", "Line 2"))
        lf.append("Line 3")
        assertEquals(lf.lines, listOf("Line 1", "Line 2", "Line 3"))
    }

    @Test
    fun testReadEmpty() {
        val lf = LinesFile(tempDir!!.resolve("child.txt"))
        assertEquals(lf.lines, listOf())
    }

    @Test
    fun testCompressAndRead() {
        val lf = LinesFile(tempDir!!.resolve("child.txt"))

        lf.append("Line 1")
        lf.append("Line 2")
        lf.append("Line 3")

        assert(!lf.isCompressed)

        lf.compress()

        assert(lf.isCompressed)
        assert(!lf.triple.dirty.exists())
        assert(!lf.triple.raw.exists())

        assertEquals(listOf("Line 1", "Line 2", "Line 3"), lf.lines)
    }

    @Test
    fun testDancing() {
        val fileText: String = ClassLoader.getSystemResource("dancing.txt").readText()

        val originalLines = fileText.lines()
        val lf = LinesFile(tempDir!!.resolve("compressed.txt"))
        assertEquals(0, lf.size)
        for (line in originalLines) {
            lf.append(line)
        }
        assertEquals(57316, lf.size)

        lf.compress()

        assertTrue(lf.size > 1000)
        assertTrue(lf.size < 40000)

        assertEquals(lf.lines, originalLines)
    }
}