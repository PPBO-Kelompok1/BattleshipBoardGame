package entities;

import input.CoordTarget;
import input.GameCallback;
import physics.Board;

public class RadarCruiser extends Ship {

    public RadarCruiser() {
        super("Radar Cruiser", 3, 2, 6, 2);
    }

    @Override
    public void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback) {
        if (!validateSkillUsage(callback)) {
            return;
        }

        callback.requestCoordinates("Radar Sweep - pick the center of a 3x3 scan", (row, col) -> {
            if (!enemyBoard.isInside(row - 1, col - 1) || !enemyBoard.isInside(row + 1, col + 1)) {
                callback.showMessage("Radar Sweep target must keep the full 3x3 area inside the board.");
                return;
            }

            int detectedSegments = 0;

            for (int r = row - 1; r <= row + 1; r++) {
                for (int c = col - 1; c <= col + 1; c++) {
                    if (!enemyBoard.isInside(r, c)) {
                        continue;
                    }

                    enemyBoard.markInteracted(r, c);

                    if (enemyBoard.getTile(r, c).hasShip()) {
                        detectedSegments++;
                    }
                }
            }

            skillUsed = true;
            callback.showMessage("Radar Sweep: detected " + detectedSegments + " ship segments in this 3x3 area.");
        }, CoordTarget.ENEMY_BOARD);
    }
}
