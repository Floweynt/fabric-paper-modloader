package com.floweytf.fabricpaperloader.paperclip;

import com.floweytf.fabricpaperloader.util.ReroutingCL;
import com.floweytf.fabricpaperloader.util.Utils;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

/**
 * A utility class for containing annoying procedural logic required for running paperclip.
 */
public class PaperclipRunner {
    private static Path paperclipPath = Path.of("");
    /**
     * Finds paperclip with Fabric's tools.
     *
     * @return Paperclip search result, or empty.
     */
    private static Optional<GameProviderHelper.FindResult> findPaperclip() {
        final var paperLocations = new ArrayList<String>();

        if (System.getProperty(SystemProperties.GAME_JAR_PATH) != null) {
            paperLocations.add(System.getProperty(SystemProperties.GAME_JAR_PATH));
        }

        // List out default locations.
        paperLocations.add("./paperclip.jar");
        paperLocations.add("./paper.jar");

        final var existingPaperLocations = paperLocations.stream()
            .map(p -> Paths.get(p).toAbsolutePath().normalize())
            .filter(Files::exists).toList();

        // Find the "correct" paper jar
        final var result = GameProviderHelper.findFirst(
            existingPaperLocations,
            new HashMap<>(),
            true,
            "io.papermc.paperclip.Main"
        );

        if (result == null || result.path == null) {
            Log.error(
                LogCategory.GAME_PROVIDER,
                "Could not locate paperclip - searched in:\n%s",
                paperLocations.stream()
                    .map(p -> (String.format("* %s", Paths.get(p).toAbsolutePath().normalize())))
                    .collect(Collectors.joining("\n"))
            );

            return Optional.empty();
        }

        return Optional.of(result);
    }

    /**
     * Invokes Paperclip's main function, returning the exit code.
     *
     * @param parent The ClassLoader to use for loading Paperclip. Must be able to load Paperclip's classes.
     * @return The exit code.
     * @throws Exception On internal reflection errors.
     */
    private static int invokePaperclip(ClassLoader parent) throws Exception {
        try {
            final var cl = new ReroutingCL(parent, name -> name.startsWith("io.papermc"))
                .rerouteS(System.class, "exit", PaperclipRunner.class, "handleExit", void.class, int.class)
                .rerouteS(Boolean.class, "getBoolean", PaperclipRunner.class, "handleGetBoolean", boolean.class,
                    String.class)
                .rerouteI(PrintStream.class, "println", PaperclipRunner.class, "handlePrint", void.class, String.class);

            Log.info(LogCategory.GAME_PROVIDER, "Running paperclip...");

            cl.loadClass("io.papermc.paperclip.Paperclip")
                .getMethod("main", String[].class)
                .invoke(null, (Object) new String[]{});
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof ExitException ex) {
                Log.debug(LogCategory.GAME_PROVIDER, "Paperclip exited with return-code: %d", ex.exitCode);
                return ex.exitCode;
            }

            throw e;
        }

        return 0;
    }

    /**
     * Launches paperclip to generate library directories and a patched minecraft jar.
     *
     * @return {@code Optional.empty()} if paperclip failed to execute, otherwise version info.
     */
    public static Optional<VersionInfo> launchPaperclip() {
        final var paperclipLocateResult = findPaperclip();

        if (paperclipLocateResult.isEmpty())
            return Optional.empty();

        // launch paperclip to transform stuff
        try (final var classLoader = Utils.classLoaderFor(paperclipLocateResult.get().path)) {
            // guess minecraft version
            final var reader = new Scanner(
                Objects.requireNonNull(classLoader.getResourceAsStream("META-INF/versions.list"))
            );

            final var hash = reader.next();
            final var version = new VersionInfo(reader.next(), hash);

            Log.info(LogCategory.GAME_PROVIDER, "Found paper %s", version);
            Log.info(LogCategory.GAME_PROVIDER, "Launching paperclip to generate patched jars");

            try {
                final var paperclipRes = invokePaperclip(classLoader);

                if (paperclipRes != 0) {
                    Log.error(LogCategory.GAME_PROVIDER, "Paperclip exited with a non-zero code");
                    return Optional.empty();
                }
                paperclipPath = paperclipLocateResult.get().path;
                return Optional.of(version);
            } catch (Exception e) {
                Log.error(LogCategory.GAME_PROVIDER, "Exception thrown while executing paperclip", e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Optional.empty();
    }

    /**
     * Exception thrown when {@code System.exit} is called (after it has been redirected)
     */
    private static class ExitException extends RuntimeException {
        public int exitCode;

        public ExitException(int exitCode) {
            this.exitCode = exitCode;
        }
    }

    /**
     * Internal, rerouted from {@code System.exit}.
     *
     * @param code The exit code.
     */
    public static void handleExit(int code) {
        throw new ExitException(code);
    }

    /**
     * Internal, rerouted from {@code System.(out|err).println}.
     *
     * @param that    The {@code PrintStream} instance.
     * @param message The message.
     */
    public static void handlePrint(PrintStream that, String message) {
        Log.info(LogCategory.GAME_PROVIDER, "[paperclip]: %s", message);
    }

    /**
     * Internal, rerouted from {@code Boolean.getBoolean}.
     *
     * @param name The name of the property.
     * @return True if @ is {@code paperclip.patchonly}, otherwise {@code Boolean.getBoolean(name)}
     */
    public static boolean handleGetBoolean(String name) {
        if (name.equals("paperclip.patchonly")) {
            return true;
        }

        return Boolean.getBoolean(name);
    }

    public static Path getPaperclipPath() {
        return paperclipPath;
    }

}
