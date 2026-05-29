package me.qmftm.floatTrial.command;

import me.qmftm.floatTrial.game.Game;
import me.qmftm.floatTrial.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameCommand implements CommandExecutor, TabCompleter {

    private final GameManager manager;

    public GameCommand(GameManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c사용법: /ft start <게임>");
                    return true;
                }
                if (manager.getCurrentGame() != null && manager.getCurrentGame().isRunning()) {
                    sender.sendMessage("§c이미 게임이 진행 중입니다: §e" + manager.getCurrentGame().getDisplayName());
                    return true;
                }
                if (!manager.hasGame(args[1])) {
                    sender.sendMessage("§c존재하지 않는 게임입니다: §e" + args[1]);
                    return true;
                }
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
                manager.start(args[1], players);
            }
            case "stop" -> {
                if (manager.getCurrentGame() == null || !manager.getCurrentGame().isRunning()) {
                    sender.sendMessage("§c진행 중인 게임이 없습니다.");
                    return true;
                }
                manager.stop();
            }
            case "list" -> {
                sender.sendMessage("§6등록된 게임 목록:");
                for (Game game : manager.getGames()) {
                    String status = (manager.getCurrentGame() == game && game.isRunning()) ? " §a[진행 중]" : "";
                    sender.sendMessage("§7- §e" + game.getDisplayName() + " §8(" + game.getId() + ")" + status);
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "list").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return manager.getGames().stream()
                    .map(Game::getId)
                    .filter(id -> id.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6[FloatTrial] §f/ft start <게임> §7- 게임 시작");
        sender.sendMessage("§6[FloatTrial] §f/ft stop §7- 게임 종료");
        sender.sendMessage("§6[FloatTrial] §f/ft list §7- 게임 목록");
    }
}
