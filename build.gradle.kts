// https://docs.gradle.org/current/userguide/jacoco_plugin.html
// https://stackoverflow.com/a/62525463

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//plugins {
//    kotlin("multiplatform") version "1.6.10"
//    id("java-library")
//    jacoco
//}

plugins {
    kotlin("jvm") version "1.6.10"
    //id("java-library")
    jacoco
    java
   // id("com.github.gmazzo.buildconfig")
}

group = "io.github.rtmigo"
version = "0.0-SNAPSHOT"

repositories {
    //jcenter()
    mavenCentral()
}



jacoco {
    toolVersion = "0.8.2"
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
//    jvm {
//        compilations.all {
//            kotlinOptions.jvmTarget = "1.8"
//        }
//        withJava()
//        testRuns["test"].executionTask.configure {
//            useJUnitPlatform()
////            jacoco {
////                destinationFile = file("${buildDir}/jacoco/test.exec")
////            }
//        }
//    }
//    js(BOTH) {
//        browser {
//            commonWebpackConfig {
//                cssSupport.enabled = true
//            }
//        }
//    }
//    val hostOs = System.getProperty("os.name")
//    val isMingwX64 = hostOs.startsWith("Windows")
//    val nativeTarget = when {
//        hostOs == "Mac OS X" -> macosX64("native")
//        hostOs == "Linux" -> linuxX64("native")
//        isMingwX64 -> mingwX64("native")
//        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
//    }

    sourceSets {
        val main by getting
        val test by getting
    }
    
//    sourceSets {
////        val commonMain by getting
////        val commonTest by getting {
////            dependencies {
////                implementation(kotlin("test"))
////            }
////        }
//        val jvmMain by getting
//        val jvmTest by getting
//        val jsMain by getting
//        val jsTest by getting
//        val nativeMain by getting
//        val nativeTest by getting
//    }
}


////////////////////////////

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}


tasks.jacocoTestReport {
//    val coverageSourceDirs = arrayOf(
//            //"src/commonMain",
//            "src/main"
//    )
//
//    val classFiles = File("${buildDir}/classes/kotlin/jvm/")
//            .walkBottomUp()
//            .toSet()
//
//    classDirectories.setFrom(classFiles)
//    sourceDirectories.setFrom(files(coverageSourceDirs))
//
//    executionData
//            .setFrom(files("${buildDir}/jacoco/jvmTest.exec"))

    reports {
        xml.required.set(false)
        csv.required.set(true)
    }
}
