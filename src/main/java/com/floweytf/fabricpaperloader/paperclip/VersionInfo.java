package com.floweytf.fabricpaperloader.paperclip;

public record VersionInfo(String version, String hash) {
    @Override
    public String toString() {
        return version + "-" + hash.substring(0, Math.min(hash.length(), 8));
    }
}
