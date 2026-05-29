package me.qmftm.floatTrial.world;

import me.qmftm.floatTrial.floatTrial;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

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
        if (hasResourceWorld(worldName)) {
            return resetAndLoad(worldName);
        }

        World world = Bukkit.getWorld(worldName);
        if (world != null) return world;

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        WorldCreator creator = new WorldCreator(worldName);
        if (!worldFolder.exists()) {
            creator.type(WorldType.FLAT);
        }
        World loaded = creator.createWorld();
        if (loaded != null) applyGameRules(loaded);
        return loaded;
    }

    private void applyGameRules(World world) {
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
    }

    private boolean hasResourceWorld(String worldName) {
        String prefix = "worlds/" + worldName + "/";
        try {
            URL codeSource = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
            File jarFile = new File(codeSource.toURI());

            if (jarFile.getName().endsWith(".jar")) {
                try (JarFile jar = new JarFile(jarFile)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.getName().startsWith(prefix) && !entry.getName().equals(prefix)) {
                            return true;
                        }
                    }
                }
            } else {
                URL url = plugin.getClass().getClassLoader().getResource(prefix);
                return url != null;
            }
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().warning("리소스 월드 확인 실패: " + e.getMessage());
        }
        return false;
    }

    private World resetAndLoad(String worldName) {
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            World lobby = Bukkit.getWorlds().get(0);
            for (Player player : existing.getPlayers()) {
                player.teleport(lobby.getSpawnLocation());
            }
            Bukkit.unloadWorld(existing, false);
        }

        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            deleteFolder(worldFolder);
        }

        copyFromResources(worldName, worldFolder);
        plugin.getLogger().info("월드 '" + worldName + "'를 리소스에서 복사했습니다.");

        World loaded = new WorldCreator(worldName).createWorld();
        if (loaded != null) applyGameRules(loaded);
        return loaded;
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
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(prefix) || name.equals(prefix)) continue;

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

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        folder.delete();
    }
}
