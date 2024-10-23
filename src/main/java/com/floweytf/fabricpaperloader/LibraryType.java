package com.floweytf.fabricpaperloader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;
import net.fabricmc.loader.impl.util.UrlUtil;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.util.ASMifier;

public enum LibraryType {
    BUNDLED_FABRIC_LOADER(LibraryCategory.LAUNCHER, UrlUtil.LOADER_CODE_SOURCE),
    PAPER_MC(LibraryCategory.GAME, "org/bukkit/craftbukkit/Main.class"),
    OW2_ASM(LibraryCategory.SYSTEM, ClassReader.class, Analyzer.class, Remapper.class, ClassNode.class, ASMifier.class);

    public final LibraryCategory category;

    @Nullable
    public final Path path;

    public final String[] classes;

    LibraryType(LibraryCategory category, Path path) {
        this.category = category;
        this.path = path;
        this.classes = new String[0];
    }

    LibraryType(LibraryCategory category, String... classes) {
        this.category = category;
        this.path = null;
        this.classes = classes;
    }

    LibraryType(LibraryCategory category, Class<?>... classes) {
        this.category = category;
        this.path = null;
        this.classes = Arrays.stream(classes)
            .map(x -> x.getName().replaceAll("\\.", "/") + ".class")
            .toArray(String[]::new);
    }
}
