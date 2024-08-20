import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("java")
}

group = "dev.jsinco.brewery"
version = "1.0-ALPHA"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.oraxen.com/releases")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.30")
    compileOnly("io.th0rgal:oraxen:1.163.0")

    implementation("com.github.Carleslc.Simple-YAML:Simple-Yaml:1.8.4")
    implementation("org.jetbrains:annotations:24.0.0")
    implementation("com.github.Anon8281:UniversalScheduler:0.1.3")

    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}


tasks {

    processResources {
        outputs.upToDateWhen { false }
        filter<ReplaceTokens>(mapOf(
            "tokens" to mapOf("version" to project.version.toString()),
            "beginToken" to "\${",
            "endToken" to "}"
        ))
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }
}