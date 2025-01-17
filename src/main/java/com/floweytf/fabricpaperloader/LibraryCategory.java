package com.floweytf.fabricpaperloader;

/**
 * The category of a library, useful for determining how classloading works.
 */
public enum LibraryCategory {
    /**
     * The launcher itself.
     */
    LAUNCHER,
    /**
     * Required for the launcher.
     */
    SYSTEM,
    /**
     * The game itself.
     */
    GAME,
    /**
     * Other libraries.
     */
    OTHER
}
