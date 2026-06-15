package entities;

import core.Direction;
import input.GameCallback;
import physics.Board;

public abstract class Ship implements Skillable {

    protected String name;
    protected int width;
    protected int height;
    protected int row;
    protected int col;
    protected Direction direction;
    protected boolean skillUsed;
    protected int hp;
    protected int maxHp;
    protected int unlockDamageThreshold;

    public Ship(String name, int width, int height, int hp, int unlockDamageThreshold) {
        this.name = name;
        this.width = width;
        this.height = height;
        direction = Direction.HORIZONTAL;
        this.hp = hp;
        this.maxHp = hp;
        this.unlockDamageThreshold = unlockDamageThreshold;
        skillUsed = false;
    }

    public void place(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public String getName() {
        return name;
    }

    public void rotate() {
        if (direction == Direction.HORIZONTAL) {
            direction = Direction.VERTICAL;
        } else {
            direction = Direction.HORIZONTAL;
        }
    }

    public int getActualWidth() {
        if (direction == Direction.HORIZONTAL) {
            return width;
        }

        return height;
    }

    public int getActualHeight() {
        if (direction == Direction.HORIZONTAL) {
            return height;
        }

        return width;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void takeDamage() {
        if (hp > 0) {
            hp--;
        }
    }

    public boolean isSunk() {
        return hp <= 0;
    }

    public int getHp() {
        return hp;
    }

    public int getDamageTaken() {
        return maxHp - hp;
    }

    public boolean isSkillUnlocked() {
        return getDamageTaken() >= unlockDamageThreshold;
    }

    public boolean canUseSkill() {
        return !isSunk() && !skillUsed && isSkillUnlocked();
    }

    public boolean countsForVictory() {
        return true;
    }

    public String getSkillStatusText() {
        if (isSunk()) {
            return "SUNK";
        }

        if (skillUsed) {
            return "USED";
        }

        if (isSkillUnlocked()) {
            return "READY";
        }

        return "LOCKED (" + getDamageTaken() + "/" + unlockDamageThreshold + " damage)";
    }

    public boolean isSkillUsed() {
        return skillUsed;
    }

    public void resetSkill() {
        skillUsed = false;
    }

    public void markSkillUsed() {
        skillUsed = true;
    }

    protected boolean validateSkillUsage(GameCallback callback) {
        if (skillUsed) {
            callback.showMessage("Skill already used!");
            return false;
        }

        if (isSunk()) {
            callback.showMessage(name + " is sunk and cannot use skill.");
            return false;
        }

        if (!isSkillUnlocked()) {
            callback.showMessage(name + " skill locked. Needs " + unlockDamageThreshold + " damage to unlock.");
            return false;
        }

        return true;
    }

    public abstract void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback);
}
