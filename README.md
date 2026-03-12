# Fabric Paper Loader

Enables the use of modern Fabric tooling to mod paper servers, allowing bytecode transformation via Mixins that are 
impossible to achieve with regular paper plugins.

## Building

Run the `build` task (`gradlew build`). The output archive is `fabric-paper-loader-[version]-all.jar`. 

## Running 

1. Download or copy the loader jar to the base directory of the server (the one with `paper.jar` or `paperclip.jar`; the server jar must be exactly named one of these).
2. Modify startup scripts to launch the loader instead of paperclip.
3. Put mods into `mods/`. 

## Development 

There is no toolchain. The best option currently is to just use paperweight. 

It is relatively easy to create IDE runs, even though there's no native gradle plugin like loom to autogen this for you.

An example mod can be found at https://github.com/LucyChroma/paper-mixins-example.

You can create an intellij run with 
- Main class: `net.fabricmc.loader.impl.launch.knot.KnotServer`
- VM args: `-Dfabric.development=true`

### Differences from Fabric 
- Targets paper
- Obfuscated runtime names (no intermediaries)

### Suppressing ServerLib complaints
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
