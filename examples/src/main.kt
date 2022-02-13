import java.nio.file.Paths
import io.github.rtmigo.linecompress.LinesDir

fun main() {
    val linesDir = LinesDir(Paths.get("/path/to/my/logs"))

    linesDir.add("Log line 1")
    linesDir.add("Log line 2")
    linesDir.add("Log line 3")

    for (line in linesDir.readLines(reverse = true)) {
        println(line)
    }
}