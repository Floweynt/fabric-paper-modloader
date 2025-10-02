package com.floweytf.fabricpaperloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;

import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.log.LogLevel;

// TODO: use fabric's own LibClassifier instead of reinventing the wheel
public class LibraryClassifier {
    private final Map<LibraryCategory, List<Path>> classifications = new EnumMap<>(LibraryCategory.class);
    private final boolean shouldLog = Log.shouldLog(LogLevel.DEBUG, LogCategory.LIB_CLASSIFICATION);
    private boolean isDone = false;

    public void addPaths(Path... paths) {
        for (Path path : paths) {
            final var type = classifySingle(path);
            if (type == null) {
                continue;
            }
            classifications.computeIfAbsent(type, ignored -> new ArrayList<>()).add(path);
            if (shouldLog) {
                Log.debug(LogCategory.LIB_CLASSIFICATION, "classified %s as %s", path, type);
            }
        }
    }

    public void done() {
        isDone = true;
    }

    private static LibraryCategory classifySingle(Path path) {
        if (Files.isDirectory(path)) {
            for (var type : LibraryType.values()) {
                if (Objects.equals(type.path, path)) {
                    return type.category;
                }

                for (String p : type.classes) {
                    if (Files.isRegularFile(path.resolve(p))) {
                        return type.category;
                    }
                }
            }
        } else {
            if (!path.toString().endsWith(".jar")) {
                return null;
            }

            try (ZipFile zf = new ZipFile(path.toFile())) {
                for (var type : LibraryType.values()) {
                    if (Objects.equals(type.path, path)) {
                        return type.category;
                    }

                    for (String p : type.classes) {
                        if (zf.getEntry(p) != null) {
                            return type.category;
                        }
                    }
                }
            } catch (ZipError | IOException e) {
                throw new RuntimeException("error reading " + path, e);
            }
        }

        return LibraryCategory.OTHER;
    }

    public Map<LibraryCategory, List<Path>> getClassifications() {
        if (!isDone) {
            Log.warn(LogCategory.LIB_CLASSIFICATION, "lib classifications accessed before expected", new Exception());
        }

        return classifications;
    }

    public List<Path> getLauncherJars() {
        return getClassifications().getOrDefault(LibraryCategory.LAUNCHER, List.of());
    }

    public List<Path> getGameJars() {
        return getClassifications().getOrDefault(LibraryCategory.GAME, List.of());
    }

    public List<Path> getSystemJars() {
        return getClassifications().getOrDefault(LibraryCategory.SYSTEM, List.of());
    }

    public List<Path> getOtherJars() {
        return getClassifications().getOrDefault(LibraryCategory.OTHER, List.of());
    }
}
