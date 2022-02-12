# [linecompress](https://github.com/rtmigo/linecompress_kt) (draft)

Kotlin/JVM library that stores text lines in GZIP-compressed files.

## Install

Add to `settings.gradle`:

```groovy
// add this:
sourceControl {
    gitRepository("ssh://git@github.com/rtmigo/linecompress_kt.git") {
        producesModule("io.github.rtmigo:linecompress")
    }
}
```

In `build.gradle`:

```groovy
dependencies {
    // add this: 
    implementation("io.github.rtmigo:linecompress") {
        version {
            branch = 'staging'
        }
    }
}    
```

<details>
  <summary>Or depend on particular version</summary>

```groovy
dependencies {
    implementation "io.github.rtmigo:linecompress:0.0.1"
    // ...
}
```
</details>

## Use

```kotlin
import io.github.rtmigo.linecompress.LinesDir

...

val linesDir = LinesDir(File("/tmp/mydir").toPath())
linesDir.append("Log line")
```