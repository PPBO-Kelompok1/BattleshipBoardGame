package entities;

import input.GameCallback;
import input.CoordTarget;
import physics.Board;
import physics.Tile;

public class Destroyer extends Ship {

    public Destroyer() {
        super("Destroyer", 2, 1, 2, 1);
    }

    @Override
    public void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback) {
        if (!validateSkillUsage(callback)) {
            return;
        }

        doStrike(enemyBoard, callback, 1);
    }

    private void doStrike(Board enemyBoard, GameCallback callback, int strikeNum) {
        if (strikeNum > 2 || !callback.canUseAttack() || callback.isGameOver()) {
            skillUsed = true;
            return;
        }

        callback.requestCoordinates("Double Strike #" + strikeNum + " - pick a tile", (row, col) -> {
            if (!callback.canUseAttack() || callback.isGameOver()) {
                skillUsed = true;
                return;
            }

            if (!enemyBoard.isInside(row, col)) {
                callback.showMessage("Invalid target. Pick again.");
                doStrike(enemyBoard, callback, strikeNum);
                return;
            }

            Tile tile = enemyBoard.getTile(row, col);

            if (tile.isAttacked()) {
                callback.showMessage("Already attacked! Pick again.");
                doStrike(enemyBoard, callback, strikeNum);
                return;
            }

            enemyBoard.attackTile(row, col);
            String result = tile.hasShip()
                    ? (enemyBoard.isShipSunk(tile.getShip()) ? tile.getShip().getName() + " SUNK!" : "HIT!")
                    : "Miss.";
            callback.showMessage("Strike #" + strikeNum + ": " + result);

            if (callback.consumeAttack()) {
                doStrike(enemyBoard, callback, strikeNum + 1);
            } else {
                skillUsed = true;
            }
        }, CoordTarget.ENEMY_BOARD);
    }
}
