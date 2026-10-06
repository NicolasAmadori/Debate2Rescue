import java.util.Locale

plugins {
    application
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
    testImplementation("junit", "junit", "6.1.3")
    implementation("it.unibo.tuprolog.argumentation:arg2p-jvm:0.16.6")
}

application {
    mainClass.set("jason.infra.centralised.RunCentralisedMAS")
}

tasks.named<JavaExec>("run") {
    args("debate2rescue.mas2j")
    standardInput = System.`in`
}
