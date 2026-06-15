package entities;

import input.GameCallback;
import input.CoordTarget;
import physics.Board;
import physics.Tile;

public class Battleship extends Ship {

    public Battleship() {
        super("Battleship", 2, 2, 4, 2);
    }

    @Override
    public void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback) {
        if (!validateSkillUsage(callback)) {
            return;
        }

        callback.requestCoordinates("Area Bombardment - pick top-left of 2x2", (row, col) -> {
            if (!callback.canUseAttack() || callback.isGameOver()) {
                return;
            }

            if (!enemyBoard.isInside(row, col) || !enemyBoard.isInside(row + 1, col + 1)) {
                callback.showMessage("Area Bombardment target must keep the full 2x2 area inside the board.");
                return;
            }

            skillUsed = true;
            StringBuilder log = new StringBuilder("Bombardment results:<br>");

            for (int r = row; r < row + 2; r++) {
                for (int c = col; c < col + 2; c++) {
                    Tile tile = enemyBoard.getTile(r, c);

                    if (tile.isAttacked()) {
                        continue;
                    }

                    enemyBoard.attackTile(r, c);
                    log.append(tile.hasShip() ? "Hit" : "Miss")
                            .append(" at (").append(r).append(",").append(c).append(")<br>");

                    if (!callback.consumeAttack()) {
                        if (callback.isGameOver()) {
                            return;
                        }

                        callback.showMessage(log.toString());
                        return;
                    }
                }
            }

            callback.showMessage(log.toString());
        }, CoordTarget.ENEMY_BOARD);
    }
}
