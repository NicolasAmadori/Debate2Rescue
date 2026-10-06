import java.util.Locale

plugins {
    java
    id("com.diffplug.spotless") version "8.10.3"
}

/*
 * Spotless plugin configuration for Java code formatting
 */
spotless {
    java {
        googleJavaFormat()
    }
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn("spotlessApply")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

group = "it.unibo.ise.debate2rescue"

sourceSets {
    main {
        resources {
            srcDir("src/main/agents")
        }
    }
}

dependencies {
    implementation("io.github.jason-lang:jason-interpreter:3.2.1")
    testImplementation("junit", "junit", "4.13.2")
    implementation("it.unibo.tuprolog.argumentation:arg2p-jvm:0.16.6")
}

file(projectDir).listFiles()?.filter { it.extension == "mas2j" }?.forEach { mas2jFile ->
    tasks.register<JavaExec>("run${mas2jFile.nameWithoutExtension.capitalized()}Mas") {
        group = "run"
        classpath = sourceSets.getByName("main").runtimeClasspath
        mainClass.set("jason.infra.centralised.RunCentralisedMAS")
        args(mas2jFile.path)
        standardInput = System.`in`
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    }
}

fun String.capitalized(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}