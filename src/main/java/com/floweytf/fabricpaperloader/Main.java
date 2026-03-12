package com.floweytf.fabricpaperloader;

import net.fabricmc.loader.impl.launch.knot.KnotServer;
import org.spongepowered.asm.util.asm.ASM;

public class Main {
    public static void main(String... args) {
        System.setProperty("fabric.skipMcProvider", "true");
        KnotServer.main(args);
    }
}
