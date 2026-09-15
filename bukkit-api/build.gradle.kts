plugins {
    `tbp-module`
    `maven-publish`
    id("java-test-fixtures")
    alias(libs.plugins.spotbugs)
}

repositories {
    mavenCentral()
    maven("https://repo.breweryteam.dev/mirror")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    api(project(":api"))
    compileOnly(libs.paper.api)
    spotbugs(libs.spotbugs)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "thebrewingproject-bukkit"
            from(components["java"])
            pom {
                name = "TheBrewingProject Bukkit API"
                description = "API for TheBrewingProject - Bukkit"
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

spotbugs {
    excludeFilter = file("${rootProject.projectDir}/spotbugs-filter.xml")
}
