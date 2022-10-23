plugins {
    kotlin("jvm") version "1.7.20"
    id("java-library")
    id("maven-publish")
    jacoco
    java
}

java {
    withSourcesJar()
    withJavadocJar()
}


group = "io.github.rtmigo"
version = "0.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("pub") {
            from(components["java"])
            pom {
                val repo = "linecompress_kt"
                name.set("linecompress")
                description.set("Kotlin/JVM library that stores text lines in GZIP-compressed files.")
                url.set("https://github.com/rtmigo/$repo")
                developers {
                    developer {
                        name.set("Artsiom iG")
                        email.set("ortemeo@gmail.com")
                    }
                }

                organization {
                    this.name.set("Revercode")
                    this.url.set("https://revercode.com")
                }

                scm {
                    connection.set("scm:git://github.com/rtmigo/$repo.git")
                    url.set("https://github.com/rtmigo/$repo")
                }
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/rtmigo/$repo/blob/HEAD/LICENSE")
                    }
                }
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Align versions of all Kotlin components
    implementation(platform(kotlin("bom")))    // Use the Kotlin JDK 8 standard library.
    implementation(kotlin("stdlib-jdk8"))

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}


kotlin {
    sourceSets {
        val main by getting
        val test by getting
    }
}

// TESTS ///////////////////////////////////////////////////////////////////////////////////////////

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(false)
        csv.required.set(true)
    }
}
