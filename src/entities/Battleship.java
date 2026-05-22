package entities;

import input.GameCallback;
import physics.Board;
import physics.Tile;

public class Battleship extends Ship {

    public Battleship() {
        super("Battleship", 2, 2, 1);
    }

    @Override
    public void useSkill(Board enemyBoard, GameCallback callback) {
        if (skillUsed) {
            callback.showMessage("Skill already used!");
            return;
        }

        callback.requestCoordinates("Area Bombardment - pick top-left of 2x2", (row, col) -> {
            skillUsed = true;
            StringBuilder log = new StringBuilder("Bombardment results:<br>");

            for (int r = row; r < row + 2; r++) {
                for (int c = col; c < col + 2; c++) {
                    try {
                        Tile tile = enemyBoard.getTile(r, c);

                        if (tile.isAttacked()) {
                            continue;
                        }

                        enemyBoard.attackTile(r, c);
                        log.append(tile.hasShip() ? "Hit" : "Miss")
                                .append(" at (").append(r).append(",").append(c).append(")<br>");
                    } catch (Exception ignored) {
                    }
                }
            }

            callback.showMessage(log.toString());
        });
    }
}
