package com.floweytf.fabricpaperloader.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;

public class Utils {
    public static URLClassLoader classLoaderFor(Path... paths) {
        return classLoaderFor(Utils.class.getClassLoader(), paths);
    }

    public static URLClassLoader classLoaderFor(ClassLoader parent, Path... paths) {
        return new URLClassLoader(
            Arrays.stream(paths).map(x -> {
                try {
                    return x.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).toArray(URL[]::new),
            parent
        );
    }

    public static String uniformPathString(Path path) {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }

    public static Path getJar(Class<?> clazz) {
        try {
            return Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
