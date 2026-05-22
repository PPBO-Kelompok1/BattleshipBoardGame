package entities;

import input.GameCallback;
import physics.Board;

public class Submarine extends Ship {

    public Submarine() {
        super("Submarine", 3, 1, 1);
    }

    @Override
    public void useSkill(Board enemyBoard, GameCallback callback) {
        if (skillUsed) {
            callback.showMessage("Skill already used!");
            return;
        }

        callback.requestCoordinates("Sonar Scan - pick top-left of 2x2 area", (row, col) -> {
            skillUsed = true;
            boolean detected = false;

            for (int r = row; r < row + 2 && !detected; r++) {
                for (int c = col; c < col + 2 && !detected; c++) {
                    try {
                        if (enemyBoard.getTile(r, c).hasShip()) {
                            detected = true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            callback.showMessage(detected ? "Sonar: SHIP DETECTED nearby!" : "Sonar: All clear.");
        });
    }
}
