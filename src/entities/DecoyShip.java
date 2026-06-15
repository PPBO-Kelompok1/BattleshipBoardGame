package entities;

import input.GameCallback;
import physics.Board;

public class DecoyShip extends Ship {

    public DecoyShip() {
        super("Decoy Ship", 2, 1, 2, 999);
    }

    @Override
    public boolean countsForVictory() {
        return false;
    }

    @Override
    public void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback) {
        callback.showMessage("Decoy Ship has no skill.");
    }
}
