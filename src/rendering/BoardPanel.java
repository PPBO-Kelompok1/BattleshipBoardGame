package rendering;

import core.Direction;
import entities.Battleship;
import entities.Carrier;
import entities.DecoyShip;
import entities.Destroyer;
import entities.PhantomCruiser;
import entities.RadarCruiser;
import entities.Ship;
import entities.Submarine;
import physics.Board;
import physics.Tile;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class BoardPanel extends JPanel {

    private static final Color PANEL_BACKGROUND = new Color(7, 18, 30);
    private static final Color VALID_PLACEMENT = new Color(73, 187, 119);
    private static final Color INVALID_PLACEMENT = new Color(184, 50, 59);
    private static final Color RECENT_ATTACK = new Color(255, 194, 41);
    private static final Color HIT_NOT_SUNK = new Color(255, 120, 60);
    private static final Color SUNK = Color.RED;
    private static final Color MISS = new Color(194, 232, 240);
    private static final Color MISS_TEXT = new Color(23, 55, 73);
    private static final Color SHIP = new Color(91, 106, 117);
    private static final Color TARGET_HOVER = new Color(84, 181, 219);
    private static final Color WATER = new Color(20, 117, 171);

    private final Board board;
    private final boolean playerBoard;
    private final TileButton[][] buttons;
    private final Supplier<Boolean> gameStartedSupplier;
    private final Supplier<Boolean> gameOverSupplier;
    private final Supplier<Boolean> playerTurnSupplier;
    private final Supplier<Boolean> revealEnemyShipsSupplier;
    private final Supplier<Ship> selectedShipSupplier;
    private final Supplier<Direction> directionSupplier;
    private final BiConsumer<Integer, Integer> clickHandler;

    public BoardPanel(
            Board board,
            boolean playerBoard,
            Supplier<Boolean> gameStartedSupplier,
            Supplier<Boolean> gameOverSupplier,
            Supplier<Boolean> playerTurnSupplier,
            Supplier<Boolean> revealEnemyShipsSupplier,
            Supplier<Ship> selectedShipSupplier,
            Supplier<Direction> directionSupplier,
            BiConsumer<Integer, Integer> clickHandler
    ) {
        super(new GridLayout(board.getRows(), board.getCols(), 2, 2));
        this.board = board;
        this.playerBoard = playerBoard;
        this.gameStartedSupplier = gameStartedSupplier;
        this.gameOverSupplier = gameOverSupplier;
        this.playerTurnSupplier = playerTurnSupplier;
        this.revealEnemyShipsSupplier = revealEnemyShipsSupplier;
        this.selectedShipSupplier = selectedShipSupplier;
        this.directionSupplier = directionSupplier;
        this.clickHandler = clickHandler;
        buttons = new TileButton[board.getRows()][board.getCols()];

        setBackground(PANEL_BACKGROUND);
        setBorder(new EmptyBorder(8, 8, 8, 8));

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                TileButton button = new TileButton(r, c);
                buttons[r][c] = button;
                add(button);
            }
        }
    }

    public void setBoardInteractionEnabled(boolean enabled) {
        setEnabled(enabled);

        for (TileButton[] row : buttons) {
            for (TileButton button : row) {
                button.setEnabled(enabled);
            }
        }
    }

    public void refresh() {
        for (TileButton[] row : buttons) {
            for (TileButton button : row) {
                styleButton(button, false);
            }
        }
    }

    private void showPlacementHover(int row, int col, boolean show) {
        if (!playerBoard || gameStartedSupplier.get() || gameOverSupplier.get()) {
            return;
        }

        refresh();

        if (!show) {
            return;
        }

        Ship ship = selectedShipSupplier.get();
        ship.setDirection(directionSupplier.get());
        boolean valid = board.canPlaceShip(ship, row, col);

        for (int r = 0; r < ship.getActualHeight(); r++) {
            for (int c = 0; c < ship.getActualWidth(); c++) {
                int nr = row + r;
                int nc = col + c;

                if (nr >= 0 && nr < buttons.length && nc >= 0 && nc < buttons[nr].length) {
                    buttons[nr][nc].setBackground(valid ? VALID_PLACEMENT : INVALID_PLACEMENT);
                }
            }
        }
    }

    private void showTargetHover(TileButton button, boolean show) {
        if (playerBoard || !gameStartedSupplier.get() || gameOverSupplier.get() || !playerTurnSupplier.get()) {
            return;
        }

        styleButton(button, show && !board.getTile(button.row, button.col).isAttacked());
    }

    private void styleButton(TileButton button, boolean hoverTarget) {
        Tile tile = board.getTile(button.row, button.col);
        TileVisualState visualState = getVisualState(tile);
        button.setText("");
        button.setForeground(Color.WHITE);

        if (visualState == TileVisualState.HIT_NOT_SUNK) {
            button.setBackground(HIT_NOT_SUNK);
            button.setText("X");
            return;
        }

        if (visualState == TileVisualState.SUNK) {
            button.setBackground(SUNK);
            button.setText("X");
            return;
        }

        if (tile.isRecentlyAttacked()) {
            button.setBackground(RECENT_ATTACK);
            button.setText(tile.hasShip() ? "X" : "O");
            return;
        }

        if (tile.isAttacked()) {
            button.setBackground(MISS);
            button.setForeground(MISS_TEXT);
            button.setText("O");
            return;
        }

        if ((playerBoard || revealEnemyShipsSupplier.get()) && tile.hasShip()) {
            button.setBackground(SHIP);
            button.setText(shipCode(tile.getShip()));
            return;
        }

        button.setBackground(hoverTarget ? TARGET_HOVER : WATER);
    }

    private TileVisualState getVisualState(Tile tile) {
        if (!tile.hasShip() || !tile.isAttacked()) {
            return TileVisualState.ALIVE;
        }

        if (tile.getShip().isSunk()) {
            return TileVisualState.SUNK;
        }

        return TileVisualState.HIT_NOT_SUNK;
    }

    private enum TileVisualState {
        ALIVE,
        HIT_NOT_SUNK,
        SUNK
    }

    private String shipCode(Ship ship) {
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

        if (ship instanceof PhantomCruiser) {
            return "P";
        }

        if (ship instanceof RadarCruiser) {
            return "R";
        }

        if (ship instanceof Carrier) {
            return "C";
        }

        return "?";
    }

    private class TileButton extends JButton {

        private final int row;
        private final int col;

        TileButton(int row, int col) {
            this.row = row;
            this.col = col;

            int maxDimension = Math.max(board.getRows(), board.getCols());
            int cellSize = Math.max(22, Math.min(42, 480 / maxDimension));
            setPreferredSize(new Dimension(cellSize, cellSize));
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
            setFont(getFont().deriveFont(Font.BOLD, 16f));
            setToolTipText(row + ", " + col);
            setAlignmentX(Component.CENTER_ALIGNMENT);

            addActionListener(e -> {
                if (!gameOverSupplier.get()) {
                    clickHandler.accept(row, col);
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (playerBoard) {
                        showPlacementHover(row, col, true);
                    } else {
                        showTargetHover(TileButton.this, true);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (playerBoard) {
                        showPlacementHover(row, col, false);
                    } else {
                        showTargetHover(TileButton.this, false);
                    }
                }
            });
        }
    }
}
