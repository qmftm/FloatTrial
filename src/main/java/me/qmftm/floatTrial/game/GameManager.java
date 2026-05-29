package me.qmftm.floatTrial.game;

import me.qmftm.floatTrial.world.WorldManager;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameManager {

    private final WorldManager worldManager;
    private final Map<String, Game> registry = new LinkedHashMap<>();
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
            player.teleport(world.getSpawnLocation());
        }

        currentGame = game;
        game.start(world, players);
        return true;
    }

    public void stop() {
        if (currentGame == null || !currentGame.isRunning()) return;
        currentGame.end();
        currentGame = null;
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
