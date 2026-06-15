package entities;

import input.CoordTarget;
import input.GameCallback;
import physics.Board;

public class Carrier extends Ship {

    public Carrier() {
        super("Carrier", 4, 2, 8, 3);
    }

    @Override
    public void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback) {
        if (!validateSkillUsage(callback)) {
            return;
        }

        if (countValidDecoyPositions(ownBoard) < 2) {
            callback.showMessage("Decoy Deployment failed. Need 2 valid empty, unscouted, unattacked locations.");
            return;
        }

        requestDecoyPosition(ownBoard, callback, 1);
    }

    private void requestDecoyPosition(Board ownBoard, GameCallback callback, int decoyNumber) {
        callback.requestCoordinates("Decoy Deployment - pick top-left for decoy #" + decoyNumber, (row, col) -> {
            DecoyShip decoy = new DecoyShip();

            if (!ownBoard.placeDecoyShip(decoy, row, col)) {
                callback.showMessage("Invalid decoy location. Pick an empty, unattacked, uninteracted 2x1 area on your board.");
                requestDecoyPosition(ownBoard, callback, decoyNumber);
                return;
            }

            if (decoyNumber == 1 && countValidDecoyPositions(ownBoard) < 1) {
                ownBoard.clearShipPlacement(decoy);
                callback.showMessage("That decoy location leaves no room for the second decoy. Pick another location.");
                requestDecoyPosition(ownBoard, callback, decoyNumber);
                return;
            }

            if (decoyNumber < 2) {
                callback.showMessage("Decoy #" + decoyNumber + " deployed. Pick location for decoy #" + (decoyNumber + 1) + ".");
                requestDecoyPosition(ownBoard, callback, decoyNumber + 1);
                return;
            }

            markSkillUsed();
            callback.showMessage("Decoy Deployment complete. 2 decoy ships deployed.");
        }, CoordTarget.OWN_BOARD);
    }

    private int countValidDecoyPositions(Board board) {
        int count = 0;

        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                if (board.canPlaceDecoyShip(new DecoyShip(), row, col)) {
                    count++;
                }
            }
        }

        return count;
    }
}
