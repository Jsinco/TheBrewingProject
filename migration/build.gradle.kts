plugins {
    kotlin("jvm") version "2.3.20"
    id("io.github.goooler.shadow") version "8.1.7"
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
}

group = "dev.jsinco.brewery"
version = project.property("version")!!

repositories {
    mavenCentral()
    maven("https://repo.breweryteam.dev/mirror")
    maven("https://repo.jsinco.dev/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://storehouse.okaeri.eu/repository/maven-public/")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    compileOnly(project(":bukkit-impl"))
    compileOnly(project(":core"))
    compileOnly("com.dre.brewery:BreweryX:3.6.0")
    compileOnly(libs.adventure.text.minimessage)
}

tasks {
    jar {
        archiveBaseName.set(rootProject.name + "Migration")
        archiveClassifier.set("incomplete")
    }

    shadowJar {
        archiveBaseName.set(rootProject.name + "Migration")
        archiveClassifier.unset()
    }
}

bukkit {
    main = "dev.jsinco.brewery.migrator.TbpMigratorPlugin"
    foliaSupported = true
    apiVersion = "1.21"
    authors = listOf("Thorinwasher")
    name = "TbpMigratorPlugin"
    depend = listOf("TheBrewingProject", "BreweryX")
}
