plugins {
    `tbp-module`
    `maven-publish`
    id("java-test-fixtures")
    alias(libs.plugins.spotbugs)
}

repositories {
    mavenCentral()
    maven("https://repo.breweryteam.dev/mirror")
    maven("https://repo.faststats.dev/releases")
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.gson)
    compileOnly(libs.guava)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.exp4j)
    compileOnly(libs.faststats.core)
    // test
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.gson)
    testImplementation(libs.guava)
    testImplementation(libs.adventure.api)

    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.adventure.api)
    spotbugs(libs.spotbugs)
}

spotbugs {
    excludeFilter = file("${rootProject.projectDir}/spotbugs-filter.xml")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "thebrewingproject-api"
            from(components["java"])
            pom {
                name = "TheBrewingProject API"
                description = "The API for TheBrewingProject - Except server native elements"
                url = "https://tbp.breweryteam.dev/docs/welcome/"
                licenses {
                    license {
                        name = "The MIT license"
                        url =
                            "https://raw.githubusercontent.com/BreweryTeam/TheBrewingProject/refs/heads/master/LICENSE"
                    }
                }
                scm {
                    connection = "scm:git:git//github.com/BreweryProject/thebrewingproject.git"
                    developerConnection = "scm:git:ssh://github.com:BreweryProject/thebrewingproject.git"
                    url = "https://github.com/BreweryProject/thebrewingproject"
                }
            }
        }
    }
    repositories {
        maven {
            name = "breweryteam"
            url = uri("https://repo.breweryteam.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }

        }
    }
}

tasks.test {
    useJUnitPlatform()
}
