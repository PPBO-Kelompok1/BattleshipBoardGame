package entities;

import input.GameCallback;
import physics.Board;

public interface Skillable {

    void useSkill(Board ownBoard, Board enemyBoard, GameCallback callback);
}
