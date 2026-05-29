package me.qmftm.floatTrial.game.impl;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.qmftm.floatTrial.floatTrial;
import me.qmftm.floatTrial.game.Game;
import me.qmftm.floatTrial.game.impl.deathnote.DNPlayer;
import me.qmftm.floatTrial.game.impl.deathnote.Phase;
import me.qmftm.floatTrial.game.impl.deathnote.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class DeathNoteGame extends Game implements Listener {

    private static final int NIGHT_SECONDS = 60;
    private static final int DAY_SECONDS = 120;

    private static final List<String> NAME_POOL = List.of(
        "김민준", "이서연", "박지호", "최유나", "정현우",
        "강다은", "윤승호", "장수아", "임동현", "한예진",
        "오준서", "서아름", "신태양", "권하늘", "황지수",
        "류민서", "조현진", "문소율", "배준혁", "고나연"
    );

    private final Map<UUID, DNPlayer> dnPlayers = new LinkedHashMap<>();
    private Phase phase = Phase.NIGHT;
    private int timeLeft;
    private BukkitTask phaseTask;
    private boolean gameEnded = false;

    private UUID waitingKiraInput = null;
    private DNPlayer pendingDeathNoteTarget = null;

    private boolean voteActive = false;
    private UUID voteNominee = null;
    private final Map<UUID, Boolean> votes = new HashMap<>();

    // ─── Game 기본 정보 ──────────────────────────────────────────

    @Override
    public String getId() { return "deathnote"; }

    @Override
    public String getDisplayName() { return "데스노트"; }

    @Override
    public Material getIcon() { return Material.WRITTEN_BOOK; }

    // ─── 게임 시작 / 종료 ─────────────────────────────────────────

    @Override
    protected void onStart(World world, List<Player> players) {
        gameEnded = false;
        Bukkit.getPluginManager().registerEvents(this, floatTrial.getInstance());

        assignRolesAndNames(players);
        announceRoles();

        Bukkit.getScheduler().runTaskLater(floatTrial.getInstance(), this::startNight, 100L);
    }

    @Override
    protected void onEnd(World world) {
        gameEnded = true;
        if (phaseTask != null) phaseTask.cancel();
        HandlerList.unregisterAll(this);

        for (DNPlayer dnp : dnPlayers.values()) {
            Player p = Bukkit.getPlayer(dnp.getUuid());
            if (p != null) p.setGameMode(GameMode.SURVIVAL);
        }

        dnPlayers.clear();
        votes.clear();
        waitingKiraInput = null;
        pendingDeathNoteTarget = null;
        voteActive = false;
        voteNominee = null;
    }

    // ─── 역할 / 이름 배정 ─────────────────────────────────────────

    private void assignRolesAndNames(List<Player> players) {
        List<Role> roles = buildRoleList(players.size());
        Collections.shuffle(roles);

        List<String> names = new ArrayList<>(NAME_POOL);
        Collections.shuffle(names);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            dnPlayers.put(p.getUniqueId(), new DNPlayer(
                p.getUniqueId(), p.getName(),
                roles.get(i), names.get(i % names.size())
            ));
        }
    }

    private List<Role> buildRoleList(int count) {
        List<Role> list = new ArrayList<>(List.of(Role.KIRA));
        if (count >= 3) list.add(Role.L);
        if (count >= 4) list.add(Role.KIRA_FOLLOWER);
        if (count >= 5) list.add(Role.POLICE);
        if (count >= 6) list.add(Role.DETECTIVE);
        if (count >= 7) list.add(Role.REPORTER);
        if (count >= 8) list.add(Role.LOVER);
        while (list.size() < count) list.add(Role.CITIZEN);
        return list;
    }

    private void announceRoles() {
        Optional<DNPlayer> kira = findRole(Role.KIRA);
        Optional<DNPlayer> follower = findRole(Role.KIRA_FOLLOWER);

        for (DNPlayer dnp : dnPlayers.values()) {
            Player p = Bukkit.getPlayer(dnp.getUuid());
            if (p == null) continue;

            p.sendMessage(Component.text("═══════════════════════════", NamedTextColor.DARK_GRAY));
            p.sendMessage(Component.text("직업: ", NamedTextColor.GRAY)
                .append(Component.text(dnp.getRole().getDisplayName(), NamedTextColor.GOLD)));
            p.sendMessage(Component.text("본명: ", NamedTextColor.GRAY)
                .append(Component.text(dnp.getRealName(), NamedTextColor.YELLOW)));

            if (dnp.getRole() == Role.KIRA)
                follower.ifPresent(f -> p.sendMessage(Component.text("추종자: ", NamedTextColor.GRAY)
                    .append(Component.text(f.getPlayerName(), NamedTextColor.RED))));
            if (dnp.getRole() == Role.KIRA_FOLLOWER)
                kira.ifPresent(k -> p.sendMessage(Component.text("키라: ", NamedTextColor.GRAY)
                    .append(Component.text(k.getPlayerName(), NamedTextColor.RED))));

            p.sendMessage(Component.text("═══════════════════════════", NamedTextColor.DARK_GRAY));
            p.showTitle(Title.title(
                Component.text(dnp.getRole().getDisplayName(), NamedTextColor.GOLD),
                Component.text(dnp.getRealName(), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        }
    }

    // ─── 낮 / 밤 사이클 ───────────────────────────────────────────

    private void startNight() {
        if (gameEnded || !isRunning()) return;
        phase = Phase.NIGHT;
        timeLeft = NIGHT_SECONDS;
        waitingKiraInput = null;
        pendingDeathNoteTarget = null;
        dnPlayers.values().forEach(p -> p.setNightAbilityUsed(false));

        broadcast(Component.text("▌ 밤이 되었습니다.", NamedTextColor.DARK_BLUE));

        getKira().ifPresent(kira -> {
            Player kp = Bukkit.getPlayer(kira.getUuid());
            if (kp == null) return;
            kp.sendMessage(Component.text(
                "데스노트에 처형할 플레이어의 본명을 채팅으로 입력하세요.", NamedTextColor.RED));
            waitingKiraInput = kira.getUuid();
        });

        sendNightGuides();
        startTimer();
    }

    private void startDay() {
        if (gameEnded || !isRunning()) return;

        // 데스노트 처형 처리
        if (pendingDeathNoteTarget != null) {
            DNPlayer target = pendingDeathNoteTarget;
            pendingDeathNoteTarget = null;
            if (target.isAlive()) killPlayer(target, "데스노트");
            if (gameEnded) return;
        }

        phase = Phase.DAY;
        timeLeft = DAY_SECONDS;
        waitingKiraInput = null;
        voteActive = false;
        votes.clear();
        voteNominee = null;

        broadcast(Component.text("▌ 낮이 되었습니다. 토론을 시작하세요.", NamedTextColor.YELLOW));
        broadcast(Component.text("/vote <닉네임>  으로 처형 대상을 지목할 수 있습니다.", NamedTextColor.GRAY));
        broadcast(Component.text("/report <닉네임>  으로 본명을 공개할 수 있습니다. (기자 전용)", NamedTextColor.GRAY));

        startTimer();
    }

    private void startTimer() {
        if (phaseTask != null) phaseTask.cancel();
        phaseTask = Bukkit.getScheduler().runTaskTimer(floatTrial.getInstance(), () -> {
            if (gameEnded || !isRunning()) { phaseTask.cancel(); return; }

            Component bar = phase == Phase.NIGHT
                ? Component.text("🌙 밤  " + timeLeft + "초", NamedTextColor.DARK_BLUE)
                : Component.text("☀ 낮  " + timeLeft + "초", NamedTextColor.YELLOW);

            for (DNPlayer dnp : dnPlayers.values()) {
                if (!dnp.isAlive()) continue;
                Player p = Bukkit.getPlayer(dnp.getUuid());
                if (p != null) p.sendActionBar(bar);
            }

            if (timeLeft-- <= 0) {
                phaseTask.cancel();
                if (phase == Phase.NIGHT) startDay();
                else startNight();
            }
        }, 0L, 20L);
    }

    private void sendNightGuides() {
        for (DNPlayer dnp : dnPlayers.values()) {
            if (!dnp.isAlive()) continue;
            Player p = Bukkit.getPlayer(dnp.getUuid());
            if (p == null) continue;
            switch (dnp.getRole()) {
                case L             -> p.sendMessage(Component.text("/dn <닉네임>  →  본명 조사", NamedTextColor.AQUA));
                case POLICE        -> p.sendMessage(Component.text("/dn <닉네임>  →  본명 한 글자 조사", NamedTextColor.AQUA));
                case DETECTIVE     -> p.sendMessage(Component.text("/dn <닉네임>  →  직업 조사", NamedTextColor.AQUA));
                case KIRA_FOLLOWER -> p.sendMessage(Component.text("/dn <닉네임>  →  L 여부 확인", NamedTextColor.AQUA));
                case LOVER         -> p.sendMessage(Component.text("/dn <닉네임>  →  연인 지정 (1회)", NamedTextColor.AQUA));
                default -> {}
            }
        }
    }

    // ─── 이벤트 핸들러 ────────────────────────────────────────────

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        DNPlayer dnp = dnPlayers.get(event.getPlayer().getUniqueId());
        if (dnp == null || !dnp.isAlive()) return;

        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        // 키라 데스노트 입력 대기
        if (waitingKiraInput != null && dnp.getUuid().equals(waitingKiraInput)) {
            event.setCancelled(true);
            waitingKiraInput = null;
            String input = msg;
            Bukkit.getScheduler().runTask(floatTrial.getInstance(), () -> handleDeathNote(input));
            return;
        }

        // 투표 찬반
        if (voteActive && !votes.containsKey(dnp.getUuid())) {
            if (msg.equals("찬성") || msg.equalsIgnoreCase("yes")) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(floatTrial.getInstance(), () -> castVote(event.getPlayer(), true));
            } else if (msg.equals("반대") || msg.equalsIgnoreCase("no")) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(floatTrial.getInstance(), () -> castVote(event.getPlayer(), false));
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        DNPlayer dnp = dnPlayers.get(event.getPlayer().getUniqueId());
        if (dnp == null) return;

        String[] parts = event.getMessage().split(" ", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/dn" -> {
                event.setCancelled(true);
                if (parts.length < 2) return;
                String target = parts[1];
                Bukkit.getScheduler().runTask(floatTrial.getInstance(),
                    () -> handleNightAbility(event.getPlayer(), dnp, target));
            }
            case "/vote" -> {
                event.setCancelled(true);
                if (phase != Phase.DAY || !dnp.isAlive() || parts.length < 2) return;
                String target = parts[1];
                Bukkit.getScheduler().runTask(floatTrial.getInstance(),
                    () -> handleVoteNomination(event.getPlayer(), dnp, target));
            }
            case "/report" -> {
                event.setCancelled(true);
                if (phase != Phase.DAY || !dnp.isAlive() || dnp.getRole() != Role.REPORTER || parts.length < 2) return;
                String target = parts[1];
                Bukkit.getScheduler().runTask(floatTrial.getInstance(),
                    () -> handleReport(event.getPlayer(), dnp, target));
            }
        }
    }

    // ─── 데스노트 ─────────────────────────────────────────────────

    private void handleDeathNote(String input) {
        Player kiraPlayer = getKira().map(k -> Bukkit.getPlayer(k.getUuid())).orElse(null);

        DNPlayer target = dnPlayers.values().stream()
            .filter(p -> p.isAlive() && p.getRealName().equals(input))
            .findFirst().orElse(null);

        if (target == null) {
            if (kiraPlayer != null)
                kiraPlayer.sendMessage(Component.text("잘못된 본명입니다.", NamedTextColor.RED));
            return;
        }

        pendingDeathNoteTarget = target;
        if (kiraPlayer != null)
            kiraPlayer.sendMessage(Component.text(
                target.getPlayerName() + "의 처형이 예약되었습니다. 날이 밝으면 효력이 발동됩니다.", NamedTextColor.RED));
    }

    // ─── 야간 능력 ────────────────────────────────────────────────

    private void handleNightAbility(Player player, DNPlayer dnp, String targetName) {
        if (phase != Phase.NIGHT || !dnp.isAlive()) return;

        if (dnp.isNightAbilityUsed()) {
            player.sendMessage(Component.text("이미 능력을 사용했습니다.", NamedTextColor.RED));
            return;
        }

        DNPlayer target = findByName(targetName);
        if (target == null) {
            player.sendMessage(Component.text("존재하지 않는 플레이어입니다.", NamedTextColor.RED));
            return;
        }
        if (target.getUuid().equals(dnp.getUuid())) {
            player.sendMessage(Component.text("자기 자신에게는 사용할 수 없습니다.", NamedTextColor.RED));
            return;
        }

        switch (dnp.getRole()) {
            case L -> {
                player.sendMessage(Component.text(target.getPlayerName() + "의 본명: ", NamedTextColor.AQUA)
                    .append(Component.text(target.getRealName(), NamedTextColor.WHITE)));
                dnp.setNightAbilityUsed(true);
            }
            case POLICE -> {
                String name = target.getRealName();
                int idx = new Random().nextInt(name.length());
                String hint = "?".repeat(idx) + name.charAt(idx) + "?".repeat(name.length() - idx - 1);
                player.sendMessage(Component.text(target.getPlayerName() + "의 본명 단서: ", NamedTextColor.AQUA)
                    .append(Component.text(hint, NamedTextColor.WHITE)));
                dnp.setNightAbilityUsed(true);
            }
            case DETECTIVE -> {
                player.sendMessage(Component.text(target.getPlayerName() + "의 직업: ", NamedTextColor.AQUA)
                    .append(Component.text(target.getRole().getDisplayName(), NamedTextColor.WHITE)));
                dnp.setNightAbilityUsed(true);
            }
            case KIRA_FOLLOWER -> {
                boolean isL = target.getRole() == Role.L;
                player.sendMessage(Component.text(
                    target.getPlayerName() + "은(는) L " + (isL ? "입니다." : "이 아닙니다."), NamedTextColor.AQUA));
                dnp.setNightAbilityUsed(true);
            }
            case LOVER -> {
                if (dnp.getLoverPartner() != null) {
                    player.sendMessage(Component.text("이미 연인이 있습니다.", NamedTextColor.RED));
                    return;
                }
                dnp.setLoverPartner(target.getUuid());
                target.setLoverPartner(dnp.getUuid());
                player.sendMessage(Component.text(
                    target.getPlayerName() + "과(와) 연인이 되었습니다.", NamedTextColor.LIGHT_PURPLE));
                dnp.setNightAbilityUsed(true);
            }
            default -> player.sendMessage(Component.text("사용할 수 있는 야간 능력이 없습니다.", NamedTextColor.GRAY));
        }
    }

    // ─── 기자 능력 ────────────────────────────────────────────────

    private void handleReport(Player player, DNPlayer dnp, String targetName) {
        if (dnp.isReporterUsed()) {
            player.sendMessage(Component.text("이미 기사를 작성했습니다.", NamedTextColor.RED));
            return;
        }
        DNPlayer target = findByName(targetName);
        if (target == null) {
            player.sendMessage(Component.text("존재하지 않는 플레이어입니다.", NamedTextColor.RED));
            return;
        }
        dnp.setReporterUsed(true);
        broadcast(Component.text("[특보] ", NamedTextColor.GOLD)
            .append(Component.text(target.getPlayerName() + "의 본명은 ", NamedTextColor.WHITE))
            .append(Component.text(target.getRealName(), NamedTextColor.YELLOW))
            .append(Component.text("로 밝혀졌습니다!", NamedTextColor.WHITE)));
    }

    // ─── 투표 ─────────────────────────────────────────────────────

    private void handleVoteNomination(Player nominator, DNPlayer nominatorData, String targetName) {
        if (voteActive) {
            nominator.sendMessage(Component.text("이미 투표가 진행 중입니다.", NamedTextColor.RED));
            return;
        }

        DNPlayer target = findByName(targetName);
        if (target == null || !target.isAlive()) {
            nominator.sendMessage(Component.text("존재하지 않는 생존 플레이어입니다.", NamedTextColor.RED));
            return;
        }

        voteActive = true;
        voteNominee = target.getUuid();
        votes.clear();
        votes.put(nominatorData.getUuid(), true);

        broadcast(Component.text(nominator.getName() + "이(가) ")
            .append(Component.text(target.getPlayerName(), NamedTextColor.RED))
            .append(Component.text("의 처형을 요청했습니다!", NamedTextColor.YELLOW)));
        broadcast(Component.text("채팅에 찬성 / 반대 를 입력하세요.", NamedTextColor.GRAY));

        Bukkit.getScheduler().runTaskLater(floatTrial.getInstance(), this::resolveVote, 600L);
    }

    private void castVote(Player voter, boolean yes) {
        DNPlayer dnp = dnPlayers.get(voter.getUniqueId());
        if (dnp == null || !dnp.isAlive() || votes.containsKey(voter.getUniqueId())) return;
        if (voteNominee != null && voter.getUniqueId().equals(voteNominee)) return;

        votes.put(voter.getUniqueId(), yes);
        voter.sendMessage(Component.text("투표: " + (yes ? "찬성" : "반대"), NamedTextColor.GRAY));

        long aliveCount = dnPlayers.values().stream().filter(DNPlayer::isAlive).count();
        if (votes.size() >= aliveCount - 1) resolveVote();
    }

    private void resolveVote() {
        if (!voteActive || voteNominee == null) return;
        voteActive = false;

        long yes = votes.values().stream().filter(v -> v).count();
        long aliveCount = dnPlayers.values().stream().filter(DNPlayer::isAlive).count();
        DNPlayer target = dnPlayers.get(voteNominee);
        voteNominee = null;
        votes.clear();

        broadcast(Component.text("투표 결과: 찬성 " + yes + " / 반대 " + (votes.size() - yes), NamedTextColor.YELLOW));

        if (target != null && yes > aliveCount / 2) {
            broadcast(Component.text(target.getPlayerName() + "이(가) 처형되었습니다.", NamedTextColor.RED));
            killPlayer(target, "처형");
        } else {
            broadcast(Component.text("처형이 부결되었습니다.", NamedTextColor.YELLOW));
        }
    }

    // ─── 사망 처리 ────────────────────────────────────────────────

    private void killPlayer(DNPlayer target, String cause) {
        if (!target.isAlive()) return;
        target.setAlive(false);

        broadcast(Component.text(target.getPlayerName() + " (", NamedTextColor.GRAY)
            .append(Component.text(target.getRealName(), NamedTextColor.WHITE))
            .append(Component.text(") 이(가) " + cause + "로 사망했습니다.", NamedTextColor.GRAY)));

        Player tp = Bukkit.getPlayer(target.getUuid());
        if (tp != null) {
            tp.setGameMode(GameMode.SPECTATOR);
            tp.showTitle(Title.title(
                Component.text("사망", NamedTextColor.RED),
                Component.text(target.getRole().getDisplayName() + "  ·  " + target.getRealName(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        }

        // 연인 연쇄 사망
        if (target.getLoverPartner() != null) {
            DNPlayer partner = dnPlayers.get(target.getLoverPartner());
            if (partner != null && partner.isAlive()) {
                killPlayer(partner, "연인의 죽음");
                return;
            }
        }

        checkWinCondition();
    }

    // ─── 승리 조건 ────────────────────────────────────────────────

    private void checkWinCondition() {
        if (gameEnded) return;

        long kiraAlive = dnPlayers.values().stream().filter(p -> p.isAlive() && p.getRole().isKiraTeam()).count();
        long citizenAlive = dnPlayers.values().stream().filter(p -> p.isAlive() && !p.getRole().isKiraTeam()).count();
        boolean lAlive = dnPlayers.values().stream().anyMatch(p -> p.isAlive() && p.getRole() == Role.L);

        if (!lAlive || kiraAlive >= citizenAlive) {
            endGame(true);
        } else if (kiraAlive == 0) {
            endGame(false);
        }
    }

    private void endGame(boolean kiraWins) {
        if (gameEnded) return;
        gameEnded = true;
        if (phaseTask != null) phaseTask.cancel();

        Component title = kiraWins
            ? Component.text("키라 팀 승리!", NamedTextColor.RED)
            : Component.text("L / 시민 팀 승리!", NamedTextColor.AQUA);

        broadcast(Component.text("══ 역할 공개 ══", NamedTextColor.GOLD));
        for (DNPlayer dnp : dnPlayers.values()) {
            broadcast(Component.text(dnp.getPlayerName() + ": ", NamedTextColor.GRAY)
                .append(Component.text(dnp.getRole().getDisplayName(), NamedTextColor.YELLOW))
                .append(Component.text("  " + dnp.getRealName(), NamedTextColor.WHITE)));
        }

        for (DNPlayer dnp : dnPlayers.values()) {
            Player p = Bukkit.getPlayer(dnp.getUuid());
            if (p == null) continue;
            p.setGameMode(GameMode.SURVIVAL);
            p.showTitle(Title.title(
                title,
                Component.text(dnp.getRole().getDisplayName() + "  ·  " + dnp.getRealName(), NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
            ));
        }

        Bukkit.getScheduler().runTaskLater(floatTrial.getInstance(), () -> {
            if (isRunning()) floatTrial.getInstance().getGameManager().stop();
        }, 100L);
    }

    // ─── 유틸 ─────────────────────────────────────────────────────

    private void broadcast(Component msg) {
        dnPlayers.values().forEach(dnp -> {
            Player p = Bukkit.getPlayer(dnp.getUuid());
            if (p != null) p.sendMessage(msg);
        });
    }

    private Optional<DNPlayer> getKira() {
        return dnPlayers.values().stream()
            .filter(p -> p.getRole() == Role.KIRA && p.isAlive()).findFirst();
    }

    private Optional<DNPlayer> findRole(Role role) {
        return dnPlayers.values().stream().filter(p -> p.getRole() == role).findFirst();
    }

    private DNPlayer findByName(String name) {
        return dnPlayers.values().stream()
            .filter(p -> p.getPlayerName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }
}
