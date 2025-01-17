plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.fabricmc.net/")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

version = "1.0.3"
group = "com.floweytf.fabricpaperloader"

val shadowImplementation: Configuration by configurations.creating

dependencies {
    compileOnly(libs.annotations)
    api(libs.fabric.loader)
    api(libs.mixin)
    api(libs.mixin.extras)
    shadow(libs.bundles.fabric)
    shadow(libs.bundles.asm)
    shadow(libs.guava)
    shadow(libs.gson)
}

sourceSets {
    main {
        java {
            srcDir("src")
        }
    }
}

tasks {
    jar {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "com.floweytf.fabricpaperloader.Main",
                    "Specification-Version" to 8.0,
                    "Multi-Release" to "true"
                )
            )
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "MonumentaMaven"
            url = when (version.toString().endsWith("SNAPSHOT")) {
                true -> uri("https://maven.playmonumenta.com/snapshots")
                false -> uri("https://maven.playmonumenta.com/releases")
            }

            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
}
