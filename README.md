# [linecompress](https://github.com/rtmigo/linecompress_kt) (draft)

Kotlin/JVM library that stores text lines in GZIP-compressed files.

Repeats the functionality of the [Python library](https://github.com/rtmigo/linecompress_py).

# Install

Edit **settings.gradle**:

```groovy
// add this:
sourceControl {
    gitRepository("ssh://git@github.com/rtmigo/linecompress_kt.git") {
        producesModule("io.github.rtmigo:linecompress")
    }
}
```

The argument to `gitRepository` may need to be rewritten with `https://github.com` 
instead of `ssh://git@github.com`


Edit **build.gradle**:

```groovy
dependencies {
    // add this: 
    implementation("io.github.rtmigo:linecompress") { version { branch = 'staging' }}
}    
```

<details>
  <summary>Or depend on particular version</summary>

Edit **build.gradle**:

```groovy
dependencies {
    // add this:     
    implementation "io.github.rtmigo:linecompress:0.0.1"
}
```

(the changes to **settings.gradle** are the same as above)
</details>

# Use

```kotlin
import java.nio.file.Paths
import io.github.rtmigo.linecompress.LinesDir

// ...

fun main() {
    val linesDir = LinesDir(Paths.get("/path/to/my/logs"))

    linesDir.add("Log line 1")
    linesDir.add("Log line 2")
    linesDir.add("Log line 3")
    
    for (line in linesDir.readLines()) {
        println(line)
    }
}
```