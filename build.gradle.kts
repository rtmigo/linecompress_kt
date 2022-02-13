// https://docs.gradle.org/current/userguide/jacoco_plugin.html
// https://stackoverflow.com/a/62525463



plugins {
    kotlin("multiplatform") version "1.6.10"
    id("java-library")
    jacoco
}

group = "io.github.rtmigo"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
}



jacoco {
    toolVersion = "0.8.2"
}

tasks.jacocoTestReport {
    val coverageSourceDirs = arrayOf(
            //"src/commonMain",
            "src/jvmMain"
    )

    val classFiles = File("${buildDir}/classes/kotlin/jvm/")
            .walkBottomUp()
            .toSet()

    classDirectories.setFrom(classFiles)
    sourceDirectories.setFrom(files(coverageSourceDirs))

    executionData
            .setFrom(files("${buildDir}/jacoco/jvmTest.exec"))

    reports {
        xml.isEnabled = true
        csv.isEnabled = true
    }
}


dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}


kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
//            jacoco {
//                destinationFile = file("${buildDir}/jacoco/test.exec")
//            }
        }
    }
    js(BOTH) {
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}
