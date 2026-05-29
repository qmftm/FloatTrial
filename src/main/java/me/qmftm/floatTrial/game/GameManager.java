package me.qmftm.floatTrial.game;

import me.qmftm.floatTrial.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final WorldManager worldManager;
    private final Map<String, Game> registry = new LinkedHashMap<>();
    private final Map<UUID, Location> savedLocations = new HashMap<>();
    private Game currentGame = null;

    public GameManager(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    public void register(Game game) {
        registry.put(game.getId().toLowerCase(), game);
    }

    public boolean start(String id, List<Player> players) {
        if (currentGame != null && currentGame.isRunning()) return false;
        Game game = registry.get(id.toLowerCase());
        if (game == null) return false;

        World world = worldManager.loadGameWorld(game.getWorldName());
        if (world == null) return false;

        for (Player player : players) {
            savedLocations.put(player.getUniqueId(), player.getLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.teleport(world.getSpawnLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        currentGame = game;
        game.start(world, players);
        return true;
    }

    public void stop() {
        if (currentGame == null || !currentGame.isRunning()) return;

        World gameWorld = currentGame.getWorld();
        if (gameWorld != null) {
            Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
            for (Player player : List.copyOf(gameWorld.getPlayers())) {
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
                Location saved = savedLocations.remove(player.getUniqueId());
                player.teleport(saved != null ? saved : fallback);
            }
        }

        currentGame.end();
        currentGame = null;
        savedLocations.clear();
    }

    public Game getCurrentGame() {
        return currentGame;
    }

    public Collection<Game> getGames() {
        return registry.values();
    }

    public boolean hasGame(String id) {
        return registry.containsKey(id.toLowerCase());
    }
}
