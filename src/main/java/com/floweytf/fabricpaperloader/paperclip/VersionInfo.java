package com.floweytf.fabricpaperloader.paperclip;

import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * The version of paper.
 *
 * @param version The Minecraft version, like 1.19.4 or 1.20.
 * @param hash    The hash of the jar, used to distinguish paper's own builds.
 */
public record VersionInfo(String version, @Nullable String hash, Path[] requiredLibraries, Path serverJarPath) {
    public String rawVersion() {
        return version + "+" + hash;
    }

    public String normalizedVersion() {
        return "%s+%s".formatted(version, hash == null ? "devel" : hash.substring(0, Math.min(hash.length(), 8)));
    }
}
