package me.qmftm.floatTrial;

import org.bukkit.plugin.java.JavaPlugin;

public final class floatTrial extends JavaPlugin {

    private static floatTrial instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        getLogger().info("FloatTrial v" + getPluginMeta().getVersion() + " 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        getLogger().info("FloatTrial이 비활성화되었습니다.");
    }

    public static floatTrial getInstance() {
        return instance;
    }
}
