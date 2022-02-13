![Generic badge](https://img.shields.io/badge/JVM-8-blue.svg)
![Generic badge](https://img.shields.io/badge/testing_on-Linux_|_Windows-blue.svg)
![JaCoCo](https://raw.github.com/rtmigo/linecompress_kt/staging/.github/badges/jacoco.svg)


# [linecompress](https://github.com/rtmigo/linecompress_kt#readme) (draft)

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

fun main() {
    val linesDir = LinesDir(Paths.get("/path/to/my/logs"))

    linesDir.add("Log line 1")
    linesDir.add("Log line 2")
    linesDir.add("Log line 3")

    // reading from oldest to newest 
    for (line in linesDir.readLines()) {
        println(line)
    }

    // reading from newest to oldest 
    for (line in linesDir.readLines(reverse = true)) {
        println(line)
    }
    
}
```

### Directory structure

```
000/000/000.txt.gz 
000/000/001.txt.gz 
000/000/002.txt.gz 
...
000/000/999.txt.gz 
000/001/000.txt.gz
...
000/001/233.txt.gz 
000/001/234.txt 
```

The last file usually contains raw text, not yet compressed.

### Limitations

The default maximum file size is 1 million bytes (decimal megabyte).

This is the size of text data *before* compression.

The directory will hold up to a billion of these files. Thus, the maximum total
storage size is one decimal petabyte.

By changing the value of the `subdirs` argument, we change the maximum number of
files: an increase in `subdirs` by one means an increase in the number of
files by a thousand times.

With the default file size 1MB we get the following limits:


| subdirs     | file path            | max sum size |
|-------------|----------------------|--------------|
| `subdirs=0` | `000.gz`             | gigabyte     |
| `subdirs=1` | `000/000.gz`         | terabyte     |
| `subdirs=2` | `000/000/000.gz`     | petabyte     |
| `subdirs=3` | `000/000/000/000.gz` | exabyte      |

These are the data sizes before compression. The actual size of the files on
the disk will most likely be smaller.

Adjusting the limits:

```kotlin
LinesDir(Paths.get("/max/1_gigabyte"), subdirs = 1)
LinesDir(Paths.get("/max/1_petabyte"))  // subdirs = 2 is the default
LinesDir(Paths.get("/max/1_exabyte"), subdirs = 3)
```

The file size can also be adjusted.

```kotlin
LinesDir(Paths.get("/max/1_petabyte"))
LinesDir(Paths.get("/max/5_petabytes", bufferSize=5*1000*1000))
```

* With larger files, we get better compression and less load on the file system
* With smaller files, we're much more efficient at iterating through lines in
  reverse order. The moment of compressing a .txt buffer into a .txt.gz archive is also faster

# See also

* [linecompress_py](https://github.com/rtmigo/linecompress_py) – a Python library