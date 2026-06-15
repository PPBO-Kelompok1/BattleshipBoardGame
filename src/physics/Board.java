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
        return ship.isSunk();
    }

    public Tile getTile(int row, int col) {
        return grid[row][col];
    }

    public boolean isInside(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public boolean wasInteracted(int row, int col) {
        return isInside(row, col) && grid[row][col].isInteracted();
    }

    public void markInteracted(int row, int col) {
        if (isInside(row, col)) {
            grid[row][col].markInteracted();
        }
    }

    public boolean canPlaceShip(Ship ship, int row, int col) {
        if (row < 0 ||
                col < 0 ||
                row + ship.getActualHeight() > rows ||
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

    public boolean canPlaceShipWithBuffer(Ship ship, int row, int col, int bufferSize) {
        if (!canPlaceShip(ship, row, col)) {
            return false;
        }

        int startRow = Math.max(0, row - bufferSize);
        int endRow = Math.min(rows - 1, row + ship.getActualHeight() + bufferSize - 1);
        int startCol = Math.max(0, col - bufferSize);
        int endCol = Math.min(cols - 1, col + ship.getActualWidth() + bufferSize - 1);

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (grid[r][c].hasShip()) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean attackTile(int row, int col) {
        if (!isInside(row, col)) {
            return false;
        }

        Tile tile = grid[row][col];

        if (tile.isAttacked()) {
            return false;
        }

        if (tile.hasShip()) {
            tile.getShip().takeDamage();
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

    public void clearShipPlacement(Ship ship) {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].getShip() == ship) {
                    grid[r][c].setShip(null);
                }
            }
        }
    }

    public boolean canRelocateShip(Ship ship, int row, int col) {
        if (!isInside(row, col) ||
                !isInside(row + ship.getActualHeight() - 1, col + ship.getActualWidth() - 1)) {
            return false;
        }

        for (int r = 0; r < ship.getActualHeight(); r++) {
            for (int c = 0; c < ship.getActualWidth(); c++) {
                Tile tile = grid[row + r][col + c];

                if (tile.hasShip() && tile.getShip() != ship) {
                    return false;
                }

                if (tile.isAttacked() || tile.isInteracted()) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean relocateShip(Ship ship, int row, int col) {
        if (!canRelocateShip(ship, row, col)) {
            return false;
        }

        clearShipPlacement(ship);
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
