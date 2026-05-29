package me.qmftm.floatTrial.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class Game {

    private boolean running = false;
    private World world = null;

    public abstract String getId();

    public abstract String getDisplayName();

    public abstract Material getIcon();

    public List<Component> getIconLore() {
        return List.of(
            Component.text("클릭하여 시작", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
        );
    }

    public String getWorldName() {
        return getId();
    }

    protected abstract void onStart(World world, List<Player> players);

    protected abstract void onEnd(World world);

    public final void start(World world, List<Player> players) {
        this.world = world;
        running = true;
        onStart(world, players);
    }

    public final void end() {
        if (!running) return;
        running = false;
        onEnd(world);
        world = null;
    }

    public final boolean isRunning() {
        return running;
    }

    public final World getWorld() {
        return world;
    }
}
