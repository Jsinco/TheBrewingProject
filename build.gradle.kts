plugins {
    id("java")
}

group = "dev.jsinco.brewery"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.oraxen.com/releases")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.30")

    //
    compileOnly("io.th0rgal:oraxen:1.163.0")

    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")
    implementation("org.jetbrains:annotations:24.0.0")

    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks.test {
    useJUnitPlatform()
}