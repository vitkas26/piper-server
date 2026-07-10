plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    id("io.ktor.plugin") version "2.3.12"
    application
}

group = "kg.nurtelecom.piper"
version = "0.1.0"

application {
    mainClass.set("kg.nurtelecom.piper.ApplicationKt")
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}
