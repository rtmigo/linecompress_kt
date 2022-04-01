import io.github.rtmigo.linecompress.DecisionBeforeAppending
import java.nio.file.Paths
import kotlin.test.*

class TestDecide {
    @Test
    fun `first subdirs 2`() {
        val d = DecisionBeforeAppending(
            root = Paths.get("/the/root"),
            subdirs = 2,
            last = null,
            lastSize = -1,
            bufferSize = -1
        )
        assertEquals(
            Paths.get("/the/root/000/000/000.txt"),
            d.rawToAppend
        )

        assertEquals(null, d.compressSource)
    }

    @Test
    fun `first subdirs 1`() {
        val d = DecisionBeforeAppending(
            root = Paths.get("/the/root"),
            subdirs = 1,
            last = null,
            lastSize = -1,
            bufferSize = -1
        )
        assertEquals(
            Paths.get("/the/root/000/000.txt"),
            d.rawToAppend
        )
        assertEquals(null, d.compressSource)
    }


    @Test
    fun `raw is small (return itself)`() {
        val d = DecisionBeforeAppending(
            root = Paths.get("/the/root"),
            subdirs = 1,
            last = Paths.get("/the/root/000/005.txt"),
            lastSize = 1000,
            bufferSize = 5000
        )
        assertEquals(
            Paths.get("/the/root/000/005.txt"),
            d.rawToAppend
        )
    }

    @Test
    fun `raw is large (compress and return next)`() {
        val d = DecisionBeforeAppending(
            root = Paths.get("/the/root"),
            subdirs = 1,
            last = Paths.get("/the/root/000/005.txt"),
            lastSize = 5005,
            bufferSize = 5000
        )

        assertEquals(
            Paths.get("/the/root/000/006.txt"),
            d.rawToAppend
        )
        assertEquals(Paths.get("/the/root/000/005.txt"), d.compressSource)
        assertEquals(Paths.get("/the/root/000/005.txt.gz"), d.compressTarget)
    }

    @Test
    fun `raw is large at 999 (compress and return next)`() {
        val d = DecisionBeforeAppending(
            root = Paths.get("/the/root"),
            subdirs = 1,
            last = Paths.get("/the/root/500/999.txt"),
            lastSize = 5005,
            bufferSize = 5000
        )

        assertEquals(
            Paths.get("/the/root/501/000.txt"),
            d.rawToAppend
        )
        assertEquals(Paths.get("/the/root/500/999.txt"), d.compressSource)
        assertEquals(Paths.get("/the/root/500/999.txt.gz"), d.compressTarget)
    }

    @Test
    fun `last is archive`() {
        val d = DecisionBeforeAppending(
            root = Paths.get("/the/root"),
            subdirs = 1,
            last = Paths.get("/the/root/123/005.txt.gz"),
            lastSize = 5005,
            bufferSize = 5000
        )

        assertEquals(
            Paths.get("/the/root/123/006.txt"),
            d.rawToAppend
        )

        assertEquals(null, d.compressSource)
        assertEquals(null, d.compressTarget)

    }
}