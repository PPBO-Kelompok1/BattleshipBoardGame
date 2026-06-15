package rendering;

import entities.Battleship;
import entities.DecoyShip;
import entities.Destroyer;
import entities.Ship;
import entities.Submarine;
import physics.Board;
import physics.Tile;
import utils.Colors;

public final class ConsoleBoardRenderer {

    private ConsoleBoardRenderer() {
    }

    public static void displayPlayerBoard(Board board) {
        System.out.print("   ");

        for (int c = 0; c < board.getCols(); c++) {
            System.out.printf("%2d ", c);
        }

        System.out.println();

        for (int r = 0; r < board.getRows(); r++) {
            System.out.printf("%2d ", r);

            for (int c = 0; c < board.getCols(); c++) {
                Tile tile = board.getTile(r, c);

                if (tile.isRecentlyAttacked()) {
                    System.out.print(Colors.parse(" {FFFF00}X "));
                } else if (tile.isAttacked() && tile.hasShip()) {
                    System.out.print(Colors.parse(" {FF0000}X "));
                } else if (tile.isAttacked()) {
                    System.out.print(Colors.parse(" {00FFFF}X "));
                } else if (tile.hasShip()) {
                    System.out.print(" " + shipCode(tile.getShip()) + " ");
                } else {
                    System.out.print(" . ");
                }
            }

            System.out.println();
        }
    }

    public static void displayHidden(Board board) {
        System.out.print("   ");

        for (int c = 0; c < board.getCols(); c++) {
            System.out.printf("%2d ", c);
        }

        System.out.println();

        for (int r = 0; r < board.getRows(); r++) {
            System.out.printf("%2d ", r);

            for (int c = 0; c < board.getCols(); c++) {
                Tile tile = board.getTile(r, c);

                if (tile.isAttacked()) {
                    System.out.print(tile.hasShip() ? " X " : " O ");
                } else {
                    System.out.print(" . ");
                }
            }

            System.out.println();
        }
    }

    private static String shipCode(Ship ship) {
        if (ship instanceof DecoyShip) {
            return "X";
        }

        if (ship instanceof Battleship) {
            return "B";
        }

        if (ship instanceof Submarine) {
            return "S";
        }

        if (ship instanceof Destroyer) {
            return "D";
        }

        return "?";
    }
}
