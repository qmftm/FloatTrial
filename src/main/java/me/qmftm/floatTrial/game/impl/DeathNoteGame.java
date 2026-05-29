package me.qmftm.floatTrial.game.impl;

import me.qmftm.floatTrial.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class DeathNoteGame extends Game {

    @Override
    public String getId() {
        return "deathnote";
    }

    @Override
    public String getDisplayName() {
        return "데스노트";
    }

    @Override
    public Material getIcon() {
        return Material.WRITTEN_BOOK;
    }

    @Override
    protected void onStart(World world, List<Player> players) {
        Bukkit.broadcastMessage("§6[FloatTrial] §c데스노트 게임이 시작됩니다!");
    }

    @Override
    protected void onEnd(World world) {
        Bukkit.broadcastMessage("§6[FloatTrial] §c데스노트 게임이 종료되었습니다.");
    }
}
