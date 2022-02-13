// https://docs.gradle.org/current/userguide/jacoco_plugin.html
// https://stackoverflow.com/a/62525463


plugins {
    kotlin("jvm") version "1.6.10"
    //id("java-library")
    jacoco
    java
}

group = "io.github.rtmigo"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    //testImplementation("org.junit.jupiter:junit-jupiter-api")
    //testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation(platform("org.junit:junit-bom:5.8.2"))
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

////////////////////////////

jacoco {
    toolVersion = "0.8.2"
}

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
