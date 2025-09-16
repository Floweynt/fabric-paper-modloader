# Fabric Paper Loader

Enables the use of modern Fabric tooling to mod paper servers, allowing bytecode transformation via Mixins that are 
impossible to achieve with regular paper plugins.

## Building

Run the `build` task (`gradlew build`). The output archive is `fabric-paper-loader.jar`. 

## Running 

1. Download or copy the loader jar to the base directory of the server (the one with `paper.jar` or `paperclip.jar`).
2. Modify startup scripts to launch the loader instead of paperclip.
3. Put mods into `mods/`. 

## Development 

There is no toolchain. The best option currently is to just use paperweight. 

It is relatively easy to create IDE runs, even though there's no native gradle plugin like loom to autogen this for you.

Example buildscript:
```kts
plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow")
    id("com.playmonumenta.paperweight-aw.userdev") version "2.0.0-build.5+2.0.0-beta.18" // from https://maven.playmonumenta.com/releases/
}

val include by configurations.creating

configurations.getByName("implementation").extendsFrom(include)
configurations.getByName("implementation").extendsFrom(configurations.getByName("mojangMappedServerRuntime"))

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.REOBF_PRODUCTION

dependencies {
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")

    implementation("io.github.llamalad7:mixinextras-common:0.5.0")
    implementation("com.floweytf.fabricpaperloader:fabric-paper-loader:2.0.0+fabric.0.17.2")

    remapper("net.fabricmc:tiny-remapper:0.11.1") {
        artifact {
            classifier = "fat"
        }
    }
}

tasks {
    jar {
        archiveClassifier.set("dev")
    }

    shadowJar {
        archiveClassifier.set("dev")
    }

    reobfJar {
		remapperArgs = TinyRemapper.createArgsList() + "--mixin"
    }
}
```

You can create an intellij run with 
- Main class: `net.fabricmc.loader.impl.launch.knot.KnotServer`
- VM args: `-Dfabric.development=true`

### Differences from Fabric 
- Targets paper
- Obfuscated runtime names (no intermediaries)

### Supressing ServerLib complaints
ServerLib mistakenly identifies this platform as a paper-over-fabric server (which it is not). This causes a warning which is printed via `System.out`, which causes bukkit to complain as well. This is annoying. You can use the following mixin:
```java
@Mixin(targets = "org.bukkit.craftbukkit.v1_20_R3.util.Commodore$1$1")
public class CommodoreMethodVisitorMixin extends MethodVisitor {
	protected CommodoreMethodVisitorMixin(int api) {
		super(api);
	}

	@Inject(
		method = "visitMethodInsn",
		at = @At("HEAD"),
		cancellable = true
	)
	private void silenceServerLib(int opcode, String owner, String name, String desc, boolean itf, CallbackInfo ci) {
		// Serverlib might be shaded...
		// This is a very safe fork with no bugs whatsoever!
		if (owner.endsWith("ServerLib") && name.equals("checkUnsafeForks")) {
			// Nuke all serverlib calls...
			ci.cancel();
		}
	}
}
``` 
