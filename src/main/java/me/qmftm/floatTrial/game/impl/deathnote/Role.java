package me.qmftm.floatTrial.game.impl.deathnote;

public enum Role {
    KIRA("키라", true),
    KIRA_FOLLOWER("키라의 추종자", true),
    L("L", false),
    POLICE("경찰", false),
    DETECTIVE("탐정", false),
    REPORTER("기자", false),
    LOVER("연인", false),
    CITIZEN("시민", false);

    private final String displayName;
    private final boolean kiraTeam;

    Role(String displayName, boolean kiraTeam) {
        this.displayName = displayName;
        this.kiraTeam = kiraTeam;
    }

    public String getDisplayName() { return displayName; }
    public boolean isKiraTeam() { return kiraTeam; }
}
