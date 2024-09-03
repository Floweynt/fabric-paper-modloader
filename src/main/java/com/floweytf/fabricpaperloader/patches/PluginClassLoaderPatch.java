package com.floweytf.fabricpaperloader.patches;

import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.loader.impl.game.patch.GamePatch;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import org.objectweb.asm.tree.ClassNode;

public class PluginClassLoaderPatch extends GamePatch {
    @Override
    public void process(FabricLauncher launcher, Function<String, ClassNode> classSource, Consumer<ClassNode> classEmitter) {

    }
}
