plugins {
    java
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

val version = "1.0.0"
val group = "com.floweytf.papermixinloader"

val shadowImplementation: Configuration by configurations.creating

dependencies {
    implementation("net.fabricmc:fabric-loader:0.16.0")
    implementation("net.fabricmc:tiny-mappings-parser:0.2.2.14")
    implementation("net.fabricmc:access-widener:2.1.0")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-analysis:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("org.ow2.asm:asm-util:9.6")
    implementation("net.fabricmc:sponge-mixin:0.15.0+mixin.0.8.7")
    implementation("com.google.guava:guava:21.0")
    implementation("com.google.code.gson:gson:2.8.7")

    // Bundle libraries
    shadow("net.fabricmc:fabric-loader:0.16.0")
    shadow("net.fabricmc:tiny-mappings-parser:0.2.2.14")
    shadow("net.fabricmc:access-widener:2.1.0")
    shadow("org.ow2.asm:asm:9.6")
    shadow("org.ow2.asm:asm-analysis:9.6")
    shadow("org.ow2.asm:asm-commons:9.6")
    shadow("org.ow2.asm:asm-tree:9.6")
    shadow("org.ow2.asm:asm-util:9.6")
    shadow("net.fabricmc:sponge-mixin:0.15.0+mixin.0.8.7")
    shadow("com.google.guava:guava:21.0")
    shadow("com.google.code.gson:gson:2.8.7")
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
                    "Main-Class" to "net.fabricmc.loader.launch.knot.KnotServer",
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
