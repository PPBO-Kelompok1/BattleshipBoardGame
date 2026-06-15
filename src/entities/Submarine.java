package entities;

import input.GameCallback;
import input.CoordTarget;
import physics.Board;

public class Submarine extends Ship {

    public Submarine() {
        super("Submarine", 3, 1, 3, 1);
    }

    @Override
    public void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback) {
        if (!validateSkillUsage(callback)) {
            return;
        }

        callback.requestCoordinates("Sonar Scan - pick top-left of 2x2 area", (row, col) -> {
            if (!enemyBoard.isInside(row, col) || !enemyBoard.isInside(row + 1, col + 1)) {
                callback.showMessage("Sonar Scan target must keep the full 2x2 area inside the board.");
                return;
            }

            skillUsed = true;
            boolean detected = false;

            for (int r = row; r < row + 2; r++) {
                for (int c = col; c < col + 2; c++) {
                    enemyBoard.markInteracted(r, c);

                    if (enemyBoard.getTile(r, c).hasShip()) {
                        detected = true;
                    }
                }
            }

            callback.showMessage(detected ? "Sonar: SHIP DETECTED nearby!" : "Sonar: All clear.");
        }, CoordTarget.ENEMY_BOARD);
    }
}
