# [linecompress](https://github.com/rtmigo/linecompress_kt) (draft)

Kotlin/JVM library that stores text lines in GZIP-compressed files.

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
</details>

# Use

```kotlin
import io.github.rtmigo.linecompress.LinesDir

// ...

fun main() {
    val linesDir = LinesDir(File("/path/to/my/logs").toPath())

    linesDir.add("Log line 1")
    linesDir.add("Log line 2")
    linesDir.add("Log line 3")
    
    for (line in linesDir.readLines()) {
        println(line)
    }
}
```