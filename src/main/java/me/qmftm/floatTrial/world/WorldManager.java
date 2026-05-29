package me.qmftm.floatTrial.world;

import me.qmftm.floatTrial.floatTrial;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class WorldManager {

    private final floatTrial plugin;

    public WorldManager(floatTrial plugin) {
        this.plugin = plugin;
    }

    public World loadGameWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) return world;

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (!worldFolder.exists()) {
            copyFromResources(worldName, worldFolder);
        }

        return new WorldCreator(worldName).createWorld();
    }

    private void copyFromResources(String worldName, File destination) {
        String prefix = "worlds/" + worldName + "/";
        try {
            URL codeSource = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            File jarFile = new File(codeSource.toURI());

            if (!jarFile.getName().endsWith(".jar")) {
                copyFromClasspath(prefix, destination);
                return;
            }

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                boolean found = false;

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(prefix) || name.equals(prefix)) continue;
                    found = true;

                    File out = new File(destination, name.substring(prefix.length()));
                    if (entry.isDirectory()) {
                        out.mkdirs();
                        continue;
                    }
                    out.getParentFile().mkdirs();
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                if (found) {
                    plugin.getLogger().info("월드 '" + worldName + "'를 리소스에서 복사했습니다.");
                }
            }
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().warning("월드 리소스 복사 실패: " + e.getMessage());
        }
    }

    private void copyFromClasspath(String prefix, File destination) {
        URL url = plugin.getClass().getClassLoader().getResource(prefix);
        if (url == null) return;

        try {
            Path source = Paths.get(url.toURI());
            Files.walk(source).forEach(p -> {
                try {
                    Path relative = source.relativize(p);
                    File out = new File(destination, relative.toString());
                    if (Files.isDirectory(p)) {
                        out.mkdirs();
                    } else {
                        out.getParentFile().mkdirs();
                        Files.copy(p, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("파일 복사 실패: " + e.getMessage());
                }
            });
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().warning("클래스패스 월드 복사 실패: " + e.getMessage());
        }
    }
}
