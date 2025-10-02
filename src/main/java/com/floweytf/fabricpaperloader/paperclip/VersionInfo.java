package com.floweytf.fabricpaperloader.paperclip;

import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * The version of paper.
 *
 * @param version The Minecraft version, like 1.19.4 or 1.20.
 * @param hash    The hash of the jar, used to distinguish paper's own builds.
 */
public record VersionInfo(
    String version,
    @Nullable String hash,
    @Nullable Path[] requiredLibraries,
    @Nullable Path serverJarPath
) {
    public String rawVersion() {
        return version + "+" + hash;
    }

    public String normalizedVersion() {
        return "%s+%s".formatted(version, hash == null ? "devel" : hash.substring(0, Math.min(hash.length(), 8)));
    }

    @Override
    public @NotNull String toString() {
        return "VersionInfo[version='%s', hash='%s', requiredLibraries=%s, serverJarPath=%s]".formatted(
            version,
            hash,
            requiredLibraries == null ? null : "[%s]".formatted(requiredLibraries.length),
            serverJarPath
        );
    }
}
