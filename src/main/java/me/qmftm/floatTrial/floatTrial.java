package me.qmftm.floatTrial;

import me.qmftm.floatTrial.command.GameCommand;
import me.qmftm.floatTrial.game.GameManager;
import me.qmftm.floatTrial.game.impl.DeathNoteGame;
import me.qmftm.floatTrial.game.impl.ShepherdBoyGame;
import me.qmftm.floatTrial.world.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class floatTrial extends JavaPlugin {

    private static floatTrial instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        gameManager = new GameManager(new WorldManager(this));
        gameManager.register(new ShepherdBoyGame());
        gameManager.register(new DeathNoteGame());

        GameCommand gameCommand = new GameCommand(gameManager);
        var ftCmd = java.util.Objects.requireNonNull(getCommand("ft"));
        ftCmd.setExecutor(gameCommand);
        ftCmd.setTabCompleter(gameCommand);

        getLogger().info("FloatTrial v" + getPluginMeta().getVersion() + " 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.stop();
        getLogger().info("FloatTrial이 비활성화되었습니다.");
    }

    public static floatTrial getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
