package entities;

import config.GameConfig;
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

    public AIPlayer(int rows, int cols, GameConfig config) {
        super(rows, cols);
        random = new Random();
        attackPlanner = new AttackPlanner(random, config);
    }

    public void setDifficulty(Difficulty difficulty) {
        attackPlanner.setDifficulty(difficulty);
    }

    public Ship randomShip() {
        int choice = random.nextInt(6);

        switch (choice) {
            case 0:
                return new Destroyer();
            case 1:
                return new Battleship();
            case 2:
                return new Submarine();
            case 3:
                return new PhantomCruiser();
            case 4:
                return new RadarCruiser();
            default:
                return new Carrier();
        }
    }

    public boolean placeShipRandomly(Ship ship) {
        Direction originalDirection = ship.getDirection();

        if (tryPlaceRandomly(ship, true) || tryPlaceRandomly(ship, false)) {
            addShip(ship);
            return true;
        }

        ship.setDirection(originalDirection);
        return false;
    }

    private boolean tryPlaceRandomly(Ship ship, boolean requireSeparation) {
        for (int attempt = 0; attempt < 500; attempt++) {
            ship.setDirection(random.nextBoolean() ? Direction.HORIZONTAL : Direction.VERTICAL);

            int row = random.nextInt(board.getRows());
            int col = random.nextInt(board.getCols());
            boolean canPlace = requireSeparation
                    ? board.canPlaceShipWithBuffer(ship, row, col, 2)
                    : board.canPlaceShip(ship, row, col);

            if (canPlace && board.placeShip(ship, row, col)) {
                return true;
            }
        }

        return false;
    }

    public void performTurn(Player target) {
        attackPlanner.performTurn(this, target);
    }

    public String getLastAiSkillMessage() {
        return attackPlanner.getLastAiSkillMessage();
    }

    public int getLastDestroyedDecoyCount() {
        return attackPlanner.getLastDestroyedDecoyCount();
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
        possibleShips.add(new PhantomCruiser());
        possibleShips.add(new RadarCruiser());
        possibleShips.add(new Carrier());
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
        if (ship == null || !ship.countsForVictory()) {
            return;
        }

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
