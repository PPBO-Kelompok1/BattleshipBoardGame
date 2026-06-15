package entities;

import physics.Board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {

    protected Board board;
    protected List<Ship> ships;

    public Player(int rows, int cols) {
        board = new Board(rows, cols);
        ships = new ArrayList<>();
    }

    public Board getBoard() {
        return board;
    }

    public List<Ship> getShips() {
        return Collections.unmodifiableList(ships);
    }

    public void addShip(Ship ship) {
        ships.add(ship);
    }

    public boolean allShipsSunk() {
        for (Ship ship : ships) {
            if (!ship.countsForVictory()) {
                continue;
            }

            if (!board.isShipSunk(ship)) {
                return false;
            }
        }

        return true;
    }
}
