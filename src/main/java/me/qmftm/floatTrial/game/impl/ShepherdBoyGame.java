package me.qmftm.floatTrial.game.impl;

import me.qmftm.floatTrial.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class ShepherdBoyGame extends Game {

    @Override
    public String getId() {
        return "shepherd";
    }

    @Override
    public String getDisplayName() {
        return "양치기 소년";
    }

    @Override
    public Material getIcon() {
        return Material.WHEAT;
    }

    @Override
    protected void onStart(World world, List<Player> players) {
        Bukkit.broadcastMessage("§6[FloatTrial] §e양치기 소년 게임이 시작됩니다!");
    }

    @Override
    protected void onEnd(World world) {
        Bukkit.broadcastMessage("§6[FloatTrial] §e양치기 소년 게임이 종료되었습니다.");
    }
}
