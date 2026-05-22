package entities;

import core.Difficulty;
import core.Direction;
import physics.Board;
import physics.Tile;
import systems.ai.AttackPlanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AIPlayer extends Player {

    private final Random random;
    private final AttackPlanner attackPlanner;

    public AIPlayer(int rows, int cols) {
        super(rows, cols);
        random = new Random();
        attackPlanner = new AttackPlanner(random);
    }

    public void setDifficulty(Difficulty difficulty) {
        attackPlanner.setDifficulty(difficulty);
    }

    public Ship randomShip() {
        int choice = random.nextInt(3);

        switch (choice) {
            case 0:
                return new Destroyer();
            case 1:
                return new Battleship();
            default:
                return new Submarine();
        }
    }

    public void placeShipRandomly(Ship ship) {
        boolean placed = false;

        while (!placed) {
            int row = random.nextInt(board.getRows());
            int col = random.nextInt(board.getCols());
            placed = board.placeShip(ship, row, col);
        }

        addShip(ship);
    }

    public void performTurn(Player target) {
        attackPlanner.performTurn(this, target);
    }

    public int getRows() {
        return board.getRows();
    }

    public int getCols() {
        return board.getCols();
    }

    public static List<Ship> possibleShipTypes() {
        List<Ship> possibleShips = new ArrayList<>();
        possibleShips.add(new Destroyer());
        possibleShips.add(new Battleship());
        possibleShips.add(new Submarine());
        return possibleShips;
    }

    public static void orientShip(Ship ship, Direction direction) {
        if (direction == Direction.VERTICAL) {
            ship.rotate();
        }
    }

    public static boolean isHit(Tile tile) {
        return tile.hasShip();
    }

    public static void discoverShip(List<Ship> targetShips, Difficulty difficulty, Ship ship, Board board) {
        if (difficulty == Difficulty.EASY) {
            return;
        }

        if (board.isShipSunk(ship)) {
            return;
        }

        if (!targetShips.contains(ship)) {
            targetShips.add(ship);
        }
    }
}
