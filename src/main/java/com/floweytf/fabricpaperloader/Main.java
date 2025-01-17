package com.floweytf.fabricpaperloader;

import net.fabricmc.loader.impl.launch.knot.KnotServer;

public class Main {
    public static void main(String... args) {
        System.setProperty("fabric.skipMcProvider", "true");
        KnotServer.main(args);
    }
}
