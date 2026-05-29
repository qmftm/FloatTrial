# FloatTrial

Paper 26.1.2 기반 멀티 미니게임 플러그인.

## 빌드

```bash
JAVA_HOME="C:/Program Files/Java/jdk-25.0.2" mvn clean package
```

출력: `D:\1.Song(삭제하지마세요)\Documents\.files\server\.26.1.2 [minigame]\plugins\floattrial-0.1.0.jar`

## 패키지 구조

```
me.qmftm.floatTrial
├── floatTrial.java          # 메인 클래스 - 게임/GUI/커맨드 초기화
├── game/
│   ├── Game.java            # 게임 추상 베이스 (getIcon, onStart, onEnd 등)
│   ├── GameManager.java     # 게임 등록·시작·종료, 플레이어 위치 저장/복원
│   └── impl/
│       ├── ShepherdBoyGame.java
│       └── DeathNoteGame.java
├── gui/
│   └── GameSelectGui.java   # 인벤토리 GUI (흰 유리판 배경, InventoryHolder 패턴)
├── world/
│   └── WorldManager.java    # 월드 로드·리셋 (리소스 번들 우선, 없으면 FLAT 생성)
└── command/
    └── GameCommand.java     # /ft - /ft start|stop|list
```

## 게임 추가 방법

1. `game/impl/` 에 `Game` 상속 클래스 생성
2. `getId()`, `getDisplayName()`, `getIcon()`, `onStart()`, `onEnd()` 구현
3. `floatTrial.java` onEnable 에 `gameManager.register(new YourGame())` 추가
4. 월드 번들이 필요하면 `src/main/resources/worlds/<게임ID>/` 에 월드 폴더 삽입

## 월드 동작 규칙

- `resources/worlds/<id>/` 존재 → 게임 시작마다 폴더 삭제 후 재복사 (리셋)
- 리소스 없음 + 폴더 없음 → FLAT 월드 신규 생성
- 리소스 없음 + 폴더 있음 → 기존 월드 그대로 로드
- 모든 게임 월드는 몹 스폰 비활성화 (`setSpawnFlags(false, false)`)

## 커맨드

| 커맨드 | 설명 |
|--------|------|
| `/ft` | 게임 선택 GUI 열기 |
| `/ft start <id>` | 게임 직접 시작 |
| `/ft stop` | 게임 종료 |
| `/ft list` | 게임 목록 |

권한: `floattrial.admin` (기본 op)

## 게임 목록

| ID | 이름 | 파일 |
|----|------|------|
| `shepherd` | 양치기 소년 | [games/shepherd.md](games/shepherd.md) |
| `deathnote` | 데스노트 | [games/deathnote.md](games/deathnote.md) |
