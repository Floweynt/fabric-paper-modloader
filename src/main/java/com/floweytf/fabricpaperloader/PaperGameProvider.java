package com.floweytf.fabricpaperloader;

import com.floweytf.fabricpaperloader.paperclip.PaperclipRunner;
import com.floweytf.fabricpaperloader.paperclip.VersionInfo;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
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
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

/**
 * A custom GameProvider which grants Fabric Loader the necessary information to launch paper.
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

    /**
     * Display an identifier for the app.
     */
    @Override
    public String getGameId() {
        return "paper";
    }

    /**
     * Display a readable name for the app.
     */
    @Override
    public String getGameName() {
        return "Paper";
    }

    /**
     * Display a raw version string that may include build numbers or git hashes.
     */
    @Override
    public String getRawGameVersion() {
        return versionInfo.rawVersion();
    }

    /**
     * Display a clean version string for display.
     */
    @Override
    public String getNormalizedGameVersion() {
        return versionInfo.normalizedVersion();
    }

    /**
     * Provides built-in mods (paper itself).
     */
    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        final var metadata = new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
            .setName(getGameName())
            .addAuthor("Paper Dev Team", Map.of())
            .setContact(new ContactInformationImpl(Map.of()))
            .setDescription("Paper minecraft server, with mixin support with fabric-loader");

        final var paper = new BuiltinMod(classifier.getGameJars(), metadata.build());

        return Collections.singletonList(paper);
    }

    /**
     * Provides the full class name of the app's entrypoint.
     */
    @Override
    public String getEntrypoint() {
        return SERVER_ENTRYPOINT;
    }

    /**
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

    /**
     * Paper was technically obf-ed, but we can simply remap mods. Post 1.20.5, paper is mojmapped.
     *
     * @return {@code false}.
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

        classifier.addPaths(UrlUtil.LOADER_CODE_SOURCE);
        launcher.getClassPath().forEach(classifier::addPaths);

        // Skip paperclip loading i.f.f. we are in a dev env, this enables us to not have paperclip.jar in run/
        // when doing an IDE run
        if (launcher.isDevelopment()) {
            classifier.done(); // early finish classifier
            final var gameJars = classifier.getClassifications().get(LibraryCategory.GAME);

            if (gameJars.isEmpty()) {
                return false; // somehow, we could not find paper in the dev env... bail.
            }

            try (
                final var fs = FileSystems.newFileSystem(gameJars.get(0));
                final var reader = Files.newBufferedReader(fs.getPath("version.json"))
            ) {
                final var object = new Gson().fromJson(reader, JsonObject.class);
                this.versionInfo = new VersionInfo(object.get("id").getAsString(), null);
            } catch (IOException e) {
                Log.error(LogCategory.DISCOVERY, "failed to find version.json from game jar", e);
                return false;
            }
        } else {
            final var paperclipResult = PaperclipRunner.launchPaperclip(); // Invoke paperclip

            if (paperclipResult.isEmpty()) {
                return false;
            }

            this.versionInfo = paperclipResult.get();

            // Scan runtime stuff
            try {
                if (!launcher.isDevelopment()) {
                    withFiles(getLaunchDirectory().resolve("libraries"), classifier::addPaths);
                    // TODO: handle this properly
                    withFiles(getLaunchDirectory().resolve("versions"), classifier::addPaths);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            classifier.done();
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

    @Override
    public GameTransformer getEntrypointTransformer() {
        return TRANSFORMER;
    }

    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        classifier.getGameJars().forEach(launcher::addToClassPath);
        classifier.getOtherJars().forEach(launcher::addToClassPath);
    }

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
        if (arguments == null) {
            return new String[0];
        }

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
