package com.floweytf.fabricpaperloader;

import com.floweytf.fabricpaperloader.paperclip.PaperclipRunner;
import com.floweytf.fabricpaperloader.paperclip.VersionInfo;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.UrlUtil;

/*
 * A custom GameProvider which grants Fabric Loader the necessary information to launch the app.
 */
public class PaperGameProvider implements GameProvider {
    public static final String SERVER_ENTRYPOINT = "org.bukkit.craftbukkit.Main";
    public static final String PROPERTY_PAPER_DIRECTORY = "gameDirectory";

    private static final GameTransformer TRANSFORMER = new PaperGameTransformer();

    private Arguments arguments;
    private final LibraryClassifier classifier = new LibraryClassifier();
    private VersionInfo versionInfo;

    private static Path getLaunchDirectory(Arguments arguments) {
        return Paths.get(arguments.getOrDefault(PROPERTY_PAPER_DIRECTORY, "."));
    }

    private void withFiles(Path path, Consumer<Path> consumer) throws IOException {
        try (final var files = Files.walk(path)) {
            files.filter(Files::isRegularFile).forEach(consumer);
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

        final var paper = new BuiltinMod(classifier.getGameJars(), metadata.build());

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
            classifier.addPaths(UrlUtil.LOADER_CODE_SOURCE);

            launcher.getClassPath().forEach(classifier::addPaths);

            if (!launcher.isDevelopment()) {
                withFiles(getLaunchDirectory().resolve("libraries"), classifier::addPaths);
                withFiles(getLaunchDirectory().resolve("versions"), classifier::addPaths); // TODO: handle this properly
            }

            classifier.done();
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
        launcher.setValidParentClassPath(classifier.getLauncherJars());
        TRANSFORMER.locateEntrypoints(launcher, classifier.getGameJars());
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
        classifier.getGameJars().forEach(launcher::addToClassPath);
        classifier.getOtherJars().forEach(launcher::addToClassPath);
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
