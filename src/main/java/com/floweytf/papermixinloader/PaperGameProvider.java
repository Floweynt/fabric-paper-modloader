package com.floweytf.papermixinloader;

import com.floweytf.papermixinloader.paperclip.PaperclipRunner;
import com.floweytf.papermixinloader.paperclip.VersionInfo;
import com.floweytf.papermixinloader.util.Utils;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;

/*
 * A custom GameProvider which grants Fabric Loader the necessary information to launch the app.
 */
public class PaperGameProvider implements GameProvider {
    public static final String SERVER_ENTRYPOINT = "org.bukkit.craftbukkit.Main";
    public static final String PROPERTY_PAPER_DIRECTORY = "gameDirectory";

    private static final GameTransformer TRANSFORMER = new PaperGameTransformer();

    private Arguments arguments;
    private List<Path> libraryJars;
    private Path selfJar;
    private Path minecraftJar;
    private VersionInfo versionInfo;

    private static Path getLaunchDirectory(Arguments arguments) {
        return Paths.get(arguments.getOrDefault(PROPERTY_PAPER_DIRECTORY, "."));
    }

    private List<Path> findFiles(Path path) throws IOException {
        try (final var files = Files.walk(path)) {
            return files.filter(Files::isRegularFile).toList();
        }
    }

    /*
     * Display an identifier for the app.
     */
    @Override
    public String getGameId() {
        return "paper";
    }

    /*
     * Display a readable name for the app.
     */
    @Override
    public String getGameName() {
        return "Paper";
    }

    /*
     * Display a raw version string that may include build numbers or git hashes.
     */
    @Override
    public String getRawGameVersion() {
        return versionInfo.toString();
    }

    /*
     * Display a clean version string for display.
     */
    @Override
    public String getNormalizedGameVersion() {
        return versionInfo.version();
    }

    /*
     * Provides built-in mods, for example a mod that represents the app itself so
     * that mods can depend on specific versions.
     */
    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        final var metadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
            .setName(getGameName())
            .addAuthor("Flowey", Map.of())
            .addAuthor("Paper dev team", Map.of())
            .setContact(new ContactInformationImpl(Map.of()))
            .setDescription("Fabric loader hack to load mixins on paper");

        final var paper = new BuiltinMod(Collections.singletonList(minecraftJar), metadata.build());

        return Collections.singletonList(paper);
    }

    /*
     * Provides the full class name of the app's entrypoint.
     */
    @Override
    public String getEntrypoint() {
        return SERVER_ENTRYPOINT;
    }

    /*
     * Provides the directory path where the app's resources (such as config) should
     * be located
     * This is where the `mods` folder will be located.
     */
    @Override
    public Path getLaunchDirectory() {
        if (arguments == null) {
            return Paths.get(".");
        }

        return getLaunchDirectory(arguments);
    }

    /*
     * Return true if the app needs to be deobfuscated.
     */
    @Override
    public boolean isObfuscated() {
        return false;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean locateGame(FabricLauncher launcher, String[] args) {
        this.arguments = new Arguments();
        this.arguments.parse(args);

        // Invoke paperclip
        final var paperclipResult = PaperclipRunner.launchPaperclip();

        if (paperclipResult.isEmpty())
            return false;

        this.versionInfo = paperclipResult.get();

        // Scan runtime stuff
        try {
            final var libDir = getLaunchDirectory().resolve("libraries");

            selfJar = Utils.getJar(getClass());

            // TODO: more robust handling of Paper's dependencies
            libraryJars = findFiles(libDir)
                .stream()
                .filter(path -> {
                    final var libIdentifier = Utils.uniformPathString(libDir.relativize(path));
                    return !libIdentifier.startsWith("org/ow2"); // don't load asm
                })
                .toList();
            minecraftJar = findFiles(getLaunchDirectory().resolve("versions")).get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    /*
     * Add additional configuration to the FabricLauncher, but do not launch your
     * app.
     */
    @Override
    public void initialize(FabricLauncher launcher) {
        launcher.setValidParentClassPath(List.of(selfJar));
        TRANSFORMER.locateEntrypoints(launcher, List.of(minecraftJar));
    }

    /*
     * Return a GameTransformer that does extra modification on the app's JAR.
     */
    @Override
    public GameTransformer getEntrypointTransformer() {
        return TRANSFORMER;
    }

    /*
     * Called after transformers were initialized and mods were detected and loaded
     * (but not initialized).
     */
    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        launcher.addToClassPath(minecraftJar);
        libraryJars.forEach(launcher::addToClassPath);
    }

    /*
     * Launch the app in this function. This MUST be done via reflection.
     */
    @Override
    public void launch(ClassLoader loader) {
        try {
            Class<?> main = loader.loadClass(this.getEntrypoint());
            Method method = main.getMethod("main", String[].class);
            method.invoke(null, (Object) this.arguments.toArray());
        } catch (InvocationTargetException e) {
            throw new FormattedException("Paper has crashed!", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new FormattedException("Failed to launch Paper", e);
        }
    }

    @Override
    public Arguments getArguments() {
        return this.arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        if (arguments == null)
            return new String[0];
        return arguments.toArray();
    }

    @Override
    public boolean canOpenErrorGui() {
        return false;
    }

    @Override
    public boolean hasAwtSupport() {
        return false;
    }
}
