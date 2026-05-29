package me.qmftm.floatTrial.game.impl.deathnote;

import java.util.UUID;

public class DNPlayer {

    private final UUID uuid;
    private final String playerName;
    private final Role role;
    private final String realName;
    private boolean alive = true;
    private boolean nightAbilityUsed = false;
    private boolean reporterUsed = false;
    private UUID loverPartner = null;

    public DNPlayer(UUID uuid, String playerName, Role role, String realName) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.role = role;
        this.realName = realName;
    }

    public UUID getUuid() { return uuid; }
    public String getPlayerName() { return playerName; }
    public Role getRole() { return role; }
    public String getRealName() { return realName; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public boolean isNightAbilityUsed() { return nightAbilityUsed; }
    public void setNightAbilityUsed(boolean used) { this.nightAbilityUsed = used; }

    public boolean isReporterUsed() { return reporterUsed; }
    public void setReporterUsed(boolean used) { this.reporterUsed = used; }

    public UUID getLoverPartner() { return loverPartner; }
    public void setLoverPartner(UUID partner) { this.loverPartner = partner; }
}
