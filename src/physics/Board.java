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

    public int calculatePlacementSpacingScore(Ship ship, int row, int col) {
        if (!canPlaceShip(ship, row, col)) {
            return -1;
        }

        int closestDistance = Integer.MAX_VALUE;
        int totalDistance = 0;
        int existingShipTiles = 0;

        for (int boardRow = 0; boardRow < rows; boardRow++) {
            for (int boardCol = 0; boardCol < cols; boardCol++) {
                if (!grid[boardRow][boardCol].hasShip()) {
                    continue;
                }

                existingShipTiles++;

                for (int shipRow = row; shipRow < row + ship.getActualHeight(); shipRow++) {
                    for (int shipCol = col; shipCol < col + ship.getActualWidth(); shipCol++) {
                        int rowDistance = Math.abs(shipRow - boardRow);
                        int colDistance = Math.abs(shipCol - boardCol);
                        int distance = Math.max(rowDistance, colDistance);

                        closestDistance = Math.min(closestDistance, distance);
                        totalDistance += distance;
                    }
                }
            }
        }

        if (existingShipTiles == 0) {
            return 0;
        }

        return closestDistance * 1000 + totalDistance;
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
