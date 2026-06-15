package config;

import core.Difficulty;

public final class GameConfig {

    public static final Difficulty DEFAULT_DIFFICULTY = Difficulty.EXTREME;
    public static final int MIN_ATTACKS_PER_ROUND = 1;
    public static final int MAX_ATTACKS_PER_ROUND = 5;
    public static final int DEFAULT_ATTACKS_PER_ROUND = 3;
    public static final int MIN_SHIPS_PER_TYPE = 1;
    public static final int MAX_SHIPS_PER_TYPE = 5;
    public static final int DEFAULT_SHIPS_PER_TYPE = 3;
    public static final int MIN_BOARD_SIZE = 10;
    public static final int MAX_BOARD_SIZE = 20;
    public static final int DEFAULT_BOARD_SIZE = 10;

    private static final GameConfig INSTANCE = new GameConfig();

    private Difficulty difficulty;
    private int attacksPerRound;
    private int shipsPerType;
    private int boardSize;

    public GameConfig() {
        resetToDefaults();
    }

    private GameConfig(GameConfig source) {
        difficulty = source.difficulty;
        attacksPerRound = source.attacksPerRound;
        shipsPerType = source.shipsPerType;
        boardSize = source.boardSize;
    }

    public static GameConfig getInstance() {
        return INSTANCE;
    }

    public GameConfig copy() {
        return new GameConfig(this);
    }

    public void resetToDefaults() {
        difficulty = DEFAULT_DIFFICULTY;
        attacksPerRound = DEFAULT_ATTACKS_PER_ROUND;
        shipsPerType = DEFAULT_SHIPS_PER_TYPE;
        boardSize = DEFAULT_BOARD_SIZE;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty == null ? DEFAULT_DIFFICULTY : difficulty;
    }

    public int getAttacksPerRound() {
        return attacksPerRound;
    }

    public void setAttacksPerRound(int attacksPerRound) {
        this.attacksPerRound = clamp(attacksPerRound, MIN_ATTACKS_PER_ROUND, MAX_ATTACKS_PER_ROUND);
    }

    public int getShipsPerType() {
        return shipsPerType;
    }

    public void setShipsPerType(int shipsPerType) {
        this.shipsPerType = clamp(shipsPerType, MIN_SHIPS_PER_TYPE, MAX_SHIPS_PER_TYPE);
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = clamp(boardSize, MIN_BOARD_SIZE, MAX_BOARD_SIZE);
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
