package entities;

import input.CoordTarget;
import input.GameCallback;
import physics.Board;

public class PhantomCruiser extends Ship {

    public PhantomCruiser() {
        super("Phantom Cruiser", 4, 1, 4, 1);
    }

    @Override
    public void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback) {
        if (!validateSkillUsage(callback)) {
            return;
        }

        callback.requestCoordinates("Shadow Relocation - pick a new top-left tile on your board", (row, col) -> {
            if (!ownBoard.canRelocateShip(this, row, col)) {
                callback.showMessage("Shadow Relocation failed. Choose an empty, untouched area inside your board.");
                return;
            }

            ownBoard.relocateShip(this, row, col);
            skillUsed = true;
            callback.showMessage("Shadow Relocation complete.");
        }, CoordTarget.OWN_BOARD);
    }
}
