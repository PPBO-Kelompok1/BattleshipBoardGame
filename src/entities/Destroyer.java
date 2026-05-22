package entities;

import input.GameCallback;
import physics.Board;
import physics.Tile;

public class Destroyer extends Ship {

    public Destroyer() {
        super("Destroyer", 2, 1, 1);
    }

    @Override
    public void useSkill(Board enemyBoard, GameCallback callback) {
        if (skillUsed) {
            callback.showMessage("Skill already used!");
            return;
        }

        skillUsed = true;
        doStrike(enemyBoard, callback, 1);
    }

    private void doStrike(Board enemyBoard, GameCallback callback, int strikeNum) {
        if (strikeNum > 2) {
            return;
        }

        callback.requestCoordinates("Double Strike #" + strikeNum + " - pick a tile", (row, col) -> {
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
            doStrike(enemyBoard, callback, strikeNum + 1);
        });
    }
}
