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
        Direction originalDirection = ship.getDirection();
        List<PlacementOption> bufferedOptions = findBestPlacementOptions(ship, true);
        List<PlacementOption> fallbackOptions = bufferedOptions.isEmpty()
                ? findBestPlacementOptions(ship, false)
                : bufferedOptions;

        if (fallbackOptions.isEmpty()) {
            ship.setDirection(originalDirection);
            return;
        }

        PlacementOption selected = fallbackOptions.get(random.nextInt(fallbackOptions.size()));
        ship.setDirection(selected.direction);
        board.placeShip(ship, selected.row, selected.col);
        addShip(ship);
    }

    private List<PlacementOption> findBestPlacementOptions(Ship ship, boolean useBuffer) {
        List<PlacementOption> bestOptions = new ArrayList<>();
        int bestScore = -1;

        Direction originalDirection = ship.getDirection();

        for (Direction direction : Direction.values()) {
            ship.setDirection(direction);

            for (int row = 0; row < board.getRows(); row++) {
                for (int col = 0; col < board.getCols(); col++) {
                    boolean canPlace = useBuffer
                            ? board.canPlaceShipWithBuffer(ship, row, col, 1)
                            : board.canPlaceShip(ship, row, col);

                    if (!canPlace) {
                        continue;
                    }

                    int score = board.calculatePlacementSpacingScore(ship, row, col);

                    if (score > bestScore) {
                        bestScore = score;
                        bestOptions.clear();
                    }

                    if (score == bestScore) {
                        bestOptions.add(new PlacementOption(row, col, direction));
                    }
                }
            }
        }

        ship.setDirection(originalDirection);
        return bestOptions;
    }

    private static class PlacementOption {
        private final int row;
        private final int col;
        private final Direction direction;

        private PlacementOption(int row, int col, Direction direction) {
            this.row = row;
            this.col = col;
            this.direction = direction;
        }
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
