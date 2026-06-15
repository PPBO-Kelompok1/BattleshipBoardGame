package physics;

import entities.Ship;

public class Tile {

    private Ship ship;
    private boolean attacked;
    private boolean recentlyAttacked;
    private boolean interacted;

    public boolean hasShip() {
        return ship != null;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    public Ship getShip() {
        return ship;
    }

    public boolean isAttacked() {
        return attacked;
    }

    public void attack() {
        attacked = true;
        interacted = true;
    }

    public boolean isInteracted() {
        return interacted;
    }

    public void setInteracted(boolean interacted) {
        this.interacted = interacted;
    }

    public void markInteracted() {
        interacted = true;
    }

    public boolean isRecentlyAttacked() {
        return recentlyAttacked;
    }

    public void setRecentlyAttacked(boolean value) {
        recentlyAttacked = value;
    }
}
