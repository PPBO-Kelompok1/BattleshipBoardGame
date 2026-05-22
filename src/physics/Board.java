package physics;

import entities.Ship;

public class Board {

    private final int rows;
    private final int cols;
    private final Tile[][] grid;

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        grid = new Tile[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Tile();
            }
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public boolean isShipSunk(Ship ship) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Tile tile = grid[r][c];

                if (tile.getShip() == ship && !tile.isAttacked()) {
                    return false;
                }
            }
        }

        return true;
    }

    public Tile getTile(int row, int col) {
        return grid[row][col];
    }

    public boolean canPlaceShip(Ship ship, int row, int col) {
        if (row + ship.getActualHeight() > rows ||
                col + ship.getActualWidth() > cols) {
            return false;
        }

        for (int r = 0; r < ship.getActualHeight(); r++) {
            for (int c = 0; c < ship.getActualWidth(); c++) {
                if (grid[row + r][col + c].hasShip()) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean attackTile(int row, int col) {
        Tile tile = grid[row][col];

        if (tile.isAttacked()) {
            return false;
        }

        tile.attack();
        return true;
    }

    public boolean placeShip(Ship ship, int row, int col) {
        if (!canPlaceShip(ship, row, col)) {
            return false;
        }

        ship.place(row, col);

        for (int r = 0; r < ship.getActualHeight(); r++) {
            for (int c = 0; c < ship.getActualWidth(); c++) {
                grid[row + r][col + c].setShip(ship);
            }
        }

        return true;
    }

    public void clearRecentAttacks() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].setRecentlyAttacked(false);
            }
        }
    }
}
