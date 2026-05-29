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

    public Ship(String name, int width, int height, int hp) {
        this.name = name;
        this.width = width;
        this.height = height;
        direction = Direction.HORIZONTAL;
        this.hp = hp;
        this.maxHp = hp;
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
        hp--;
    }

    public boolean isSunk() {
        return hp <= 0;
    }

    public int getHp() {
        return hp;
    }

    public boolean isSkillUsed() {
        return skillUsed;
    }

    public void resetSkill() {
        skillUsed = false;
    }

    public abstract void useSkill(Board enemyBoard, GameCallback callback);
}
