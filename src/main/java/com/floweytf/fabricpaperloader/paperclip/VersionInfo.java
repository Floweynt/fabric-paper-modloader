package com.floweytf.fabricpaperloader.paperclip;

/**
 * The version of paper.
 *
 * @param version The Minecraft version, like 1.19.4 or 1.20.
 * @param hash    The hash of the jar, used to distinguish paper's own builds.
 */
public record VersionInfo(String version, String hash) {
    @Override
    public String toString() {
        return version + "-" + hash.substring(0, Math.min(hash.length(), 8));
    }
}
