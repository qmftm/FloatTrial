package me.qmftm.floatTrial.gui;

import me.qmftm.floatTrial.game.Game;
import me.qmftm.floatTrial.game.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSelectGui implements InventoryHolder, Listener {

    private static final int ROWS = 3;
    private static final Component TITLE = Component.text("게임 선택", NamedTextColor.DARK_GRAY)
            .decoration(TextDecoration.ITALIC, false);

    private final GameManager manager;
    private final Map<Integer, String> slotToGame = new HashMap<>();

    public GameSelectGui(GameManager manager) {
        this.manager = manager;

        List<Game> games = new ArrayList<>(manager.getGames());
        int[] slots = calcSlots(games.size());
        for (int i = 0; i < games.size(); i++) {
            slotToGame.put(slots[i], games.get(i).getId());
        }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(this, ROWS * 9, TITLE);

        ItemStack filler = filler();
        for (int i = 0; i < ROWS * 9; i++) {
            inv.setItem(i, filler);
        }

        List<Game> games = new ArrayList<>(manager.getGames());
        int[] slots = calcSlots(games.size());
        for (int i = 0; i < games.size(); i++) {
            inv.setItem(slots[i], icon(games.get(i)));
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GameSelectGui)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        String gameId = slotToGame.get(event.getSlot());
        if (gameId == null) return;

        player.closeInventory();

        if (manager.getCurrentGame() != null && manager.getCurrentGame().isRunning()) {
            player.sendMessage(Component.text("이미 게임이 진행 중입니다: ", NamedTextColor.RED)
                    .append(Component.text(manager.getCurrentGame().getDisplayName(), NamedTextColor.YELLOW)));
            return;
        }

        manager.start(gameId, new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    private ItemStack filler() {
        ItemStack item = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack icon(Game game) {
        ItemStack item = new ItemStack(game.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(game.getDisplayName(), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(game.getIconLore());
        item.setItemMeta(meta);
        return item;
    }

    private int[] calcSlots(int count) {
        return switch (count) {
            case 1 -> new int[]{13};
            case 2 -> new int[]{11, 15};
            case 3 -> new int[]{10, 13, 16};
            case 4 -> new int[]{10, 12, 14, 16};
            default -> {
                int[] s = new int[count];
                for (int i = 0; i < count; i++) s[i] = 9 + i;
                yield s;
            }
        };
    }
}
