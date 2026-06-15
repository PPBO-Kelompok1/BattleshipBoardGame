package core;

import config.GameConfig;
import entities.AIPlayer;
import entities.Battleship;
import entities.Carrier;
import entities.DecoyShip;
import entities.Destroyer;
import entities.PhantomCruiser;
import entities.Player;
import entities.RadarCruiser;
import entities.Ship;
import entities.Submarine;
import input.CoordConsumer;
import input.CoordTarget;
import input.GameCallback;
import physics.Tile;
import rendering.BoardPanel;
import rendering.FriendlyEnumRenderer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Random;

public class Game extends JFrame implements GameCallback {

    private final Player player;
    private final AIPlayer ai;
    private final BoardPanel playerBoardPanel;
    private final BoardPanel enemyBoardPanel;
    private final JComboBox<Difficulty> difficultyBox;
    private final JComboBox<String> shipBox;
    private final JComboBox<Direction> directionBox;
    private final JComboBox<String> skillShipBox;
    private final JLabel statusLabel;
    private final JLabel turnLabel;
    private final JLabel setupLabel;

    private int placedShips;
    private int attacksLeft;
    private boolean gameStarted;
    private boolean playerTurn;
    private boolean revealEnemyShips;
    private boolean awaitingSkillInput;
    private CoordTarget pendingCoordTarget;
    private CoordConsumer pendingCoordConsumer;

    public Game() {
        super("Battleship Swing");

        player = new Player(GameConfig.BOARD_SIZE, GameConfig.BOARD_SIZE);
        ai = new AIPlayer(GameConfig.BOARD_SIZE, GameConfig.BOARD_SIZE);

        difficultyBox = new JComboBox<>(Difficulty.values());
        shipBox = new JComboBox<>(new String[]{
                "Destroyer (2x1)",
                "Battleship (2x2)",
                "Submarine (3x1)",
                "Phantom Cruiser (4x1)",
                "Radar Cruiser (3x2)",
                "Carrier (4x2)"
        });
        directionBox = new JComboBox<>(Direction.values());
        skillShipBox = new JComboBox<>();
        statusLabel = new JLabel("Choose difficulty, then place 6 ships on your board.");
        turnLabel = new JLabel("Setup");
        setupLabel = new JLabel("Ships placed: 0 / " + GameConfig.SHIP_COUNT);

        playerBoardPanel = new BoardPanel(
                player.getBoard(),
                true,
                GameConfig.BOARD_SIZE,
                () -> gameStarted,
                () -> playerTurn,
                () -> revealEnemyShips,
                this::createSelectedShip,
                () -> (Direction) directionBox.getSelectedItem(),
                this::handlePlayerBoardClick
        );
        enemyBoardPanel = new BoardPanel(
                ai.getBoard(),
                false,
                GameConfig.BOARD_SIZE,
                () -> gameStarted,
                () -> playerTurn,
                () -> revealEnemyShips,
                this::createSelectedShip,
                () -> (Direction) directionBox.getSelectedItem(),
                this::handleEnemyBoardClick
        );

        difficultyBox.setRenderer(new FriendlyEnumRenderer<>());
        directionBox.setRenderer(new FriendlyEnumRenderer<>());

        buildUi();
        refreshBoards();
    }

    @Override
    public void requestCoordinates(String prompt, CoordConsumer consumer, CoordTarget target) {
        awaitingSkillInput = true;
        pendingCoordTarget = target;
        pendingCoordConsumer = consumer;
        String boardName = target == CoordTarget.OWN_BOARD ? "your board" : "the AI board";
        statusLabel.setText(prompt + " - click a tile on " + boardName + ".");
    }

    @Override
    public void showMessage(String message) {
        statusLabel.setText("<html>" + message + "</html>");
    }

    public void start() {
        setVisible(true);
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);

        JButton skillButton = new JButton("Use Skill");
        skillButton.addActionListener(e -> {
            if (!gameStarted || !playerTurn) {
                return;
            }

            int idx = skillShipBox.getSelectedIndex();

            if (idx < 0 || idx >= player.getShips().size()) {
                return;
            }

            Ship ship = player.getShips().get(idx);
            ship.useSkill(player.getBoard(), ai.getBoard(), this);
            refreshSkillShipBox();
        });

        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(12, 28, 43));
        setContentPane(root);

        JPanel top = new JPanel(new BorderLayout(12, 12));
        top.setOpaque(false);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.add(controlLabel("Difficulty"));
        controls.add(difficultyBox);
        controls.add(controlLabel("Ship"));
        controls.add(shipBox);
        controls.add(controlLabel("Direction"));
        controls.add(directionBox);
        controls.add(controlLabel("Skill"));
        controls.add(skillShipBox);
        controls.add(skillButton);

        JButton rotateButton = new JButton("Rotate");
        rotateButton.addActionListener(e -> toggleDirection());
        controls.add(rotateButton);

        JButton restartButton = new JButton("New Game");
        restartButton.addActionListener(e -> restart());
        controls.add(restartButton);

        styleHeaderLabel(statusLabel, 16);
        styleHeaderLabel(turnLabel, 18);
        styleHeaderLabel(setupLabel, 14);

        JPanel labels = new JPanel(new GridLayout(3, 1, 2, 2));
        labels.setOpaque(false);
        labels.add(turnLabel);
        labels.add(statusLabel);
        labels.add(setupLabel);

        top.add(controls, BorderLayout.NORTH);
        top.add(labels, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);

        JPanel boards = new JPanel(new GridLayout(1, 2, 18, 0));
        boards.setOpaque(false);
        boards.add(wrapBoard("Player Board", playerBoardPanel));
        boards.add(wrapBoard("AI Board", enemyBoardPanel));
        root.add(boards, BorderLayout.CENTER);

        pack();
    }

    private JPanel wrapBoard(String title, BoardPanel panel) {
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        styleHeaderLabel(label, 18);

        JPanel wrapper = new JPanel(new BorderLayout(0, 10));
        wrapper.setOpaque(false);
        wrapper.add(label, BorderLayout.NORTH);
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private void styleHeaderLabel(JLabel label, int size) {
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, size));
    }

    private JLabel controlLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        return label;
    }

    private void toggleDirection() {
        Direction selected = (Direction) directionBox.getSelectedItem();
        directionBox.setSelectedItem(selected == Direction.HORIZONTAL ? Direction.VERTICAL : Direction.HORIZONTAL);
    }

    private void restart() {
        dispose();
        SwingUtilities.invokeLater(() -> new Game().start());
    }

    private void handlePlayerBoardClick(int row, int col) {
        if (gameStarted) {
            if (awaitingSkillInput && pendingCoordTarget == CoordTarget.OWN_BOARD && pendingCoordConsumer != null) {
                awaitingSkillInput = false;
                pendingCoordTarget = null;
                CoordConsumer consumer = pendingCoordConsumer;
                pendingCoordConsumer = null;
                consumer.accept(row, col);
                refreshSkillShipBox();
                refreshBoards();
                return;
            }

            if (awaitingSkillInput) {
                statusLabel.setText("That skill is waiting for a target on the AI board.");
            }

            return;
        }

        Ship ship = createSelectedShip();
        ship.setDirection((Direction) directionBox.getSelectedItem());

        if (!player.getBoard().placeShip(ship, row, col)) {
            statusLabel.setText("Invalid placement. Ships cannot overlap or leave the board.");
            return;
        }

        player.addShip(ship);
        placedShips++;
        setupLabel.setText("Ships placed: " + placedShips + " / " + GameConfig.SHIP_COUNT);
        statusLabel.setText(ship.getName() + " placed. Choose the next ship and tile.");

        if (placedShips == GameConfig.SHIP_COUNT) {
            finishSetup();
        }

        refreshBoards();
    }

    private void finishSetup() {
        difficultyBox.setEnabled(false);
        shipBox.setEnabled(false);
        directionBox.setEnabled(false);
        ai.setDifficulty((Difficulty) difficultyBox.getSelectedItem());
        placeAiShips();
        gameStarted = true;
        playerTurn = true;
        attacksLeft = GameConfig.ATTACKS_PER_TURN;
        turnLabel.setText("Player Turn");
        setupLabel.setText("Attack the concealed AI board.");
        statusLabel.setText("Game start. You have " + attacksLeft + " attacks.");
        refreshSkillShipBox();
    }

    private void placeAiShips() {
        Random random = new Random();

        for (int i = 0; i < GameConfig.SHIP_COUNT; i++) {
            Ship ship = ai.randomShip();

            if (random.nextBoolean()) {
                ship.rotate();
            }

            ai.placeShipRandomly(ship);
        }
    }

    private Ship createSelectedShip() {
        int selected = shipBox.getSelectedIndex();

        return switch (selected) {
            case 1 -> new Battleship();
            case 2 -> new Submarine();
            case 3 -> new PhantomCruiser();
            case 4 -> new RadarCruiser();
            case 5 -> new Carrier();
            default -> new Destroyer();
        };
    }

    private void checkWinCondition() {
        if (ai.allShipsSunk()) {
            revealEnemyShips = true;
            endGame("Player wins. All AI ships are sunk.");
        }
    }

    private void handleEnemyBoardClick(int row, int col) {
        if (awaitingSkillInput && pendingCoordTarget == CoordTarget.ENEMY_BOARD && pendingCoordConsumer != null) {
            awaitingSkillInput = false;
            pendingCoordTarget = null;
            CoordConsumer consumer = pendingCoordConsumer;
            pendingCoordConsumer = null;
            consumer.accept(row, col);
            refreshSkillShipBox();
            refreshBoards();
            checkWinCondition();
            return;
        }

        if (awaitingSkillInput) {
            statusLabel.setText("That skill is waiting for a target on your board.");
            return;
        }

        if (!gameStarted || !playerTurn) {
            return;
        }

        Tile tile = ai.getBoard().getTile(row, col);

        if (tile.isAttacked()) {
            statusLabel.setText("That tile was already attacked. Pick another target.");
            return;
        }

        ai.getBoard().attackTile(row, col);
        attacksLeft--;

        if (tile.hasShip()) {
            Ship ship = tile.getShip();

            if (ai.getBoard().isShipSunk(ship)) {
                if (ship instanceof DecoyShip || !ship.countsForVictory()) {
                    statusLabel.setText("Red explosion. You destroyed an enemy Decoy Ship! It was not an objective ship.");
                } else {
                    statusLabel.setText("Red explosion. You sunk an enemy " + ship.getName() + "!");
                }
            } else {
                statusLabel.setText("Red explosion. Hit on an enemy ship.");
            }
        } else {
            statusLabel.setText("Miss. " + attacksLeft + " attacks left.");
        }

        refreshBoards();
        checkWinCondition();

        if (!gameStarted) {
            return;
        }

        if (attacksLeft == 0) {
            startAiTurn();
        } else {
            turnLabel.setText("Player Turn - " + attacksLeft + " attacks left");
        }
    }

    private void startAiTurn() {
        playerTurn = false;
        turnLabel.setText("AI Turn");
        statusLabel.setText("AI is targeting your board...");

        Timer timer = new Timer(650, e -> {
            ((Timer) e.getSource()).stop();
            ai.performTurn(player);
            showAiAttackResult();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showAiAttackResult() {
        int hits = 0;
        int misses = 0;

        for (int r = 0; r < GameConfig.BOARD_SIZE; r++) {
            for (int c = 0; c < GameConfig.BOARD_SIZE; c++) {
                Tile tile = player.getBoard().getTile(r, c);

                if (tile.isRecentlyAttacked()) {
                    if (tile.hasShip()) {
                        hits++;
                    } else {
                        misses++;
                    }
                }
            }
        }

        refreshBoards();
        refreshSkillShipBox();

        if (player.allShipsSunk()) {
            revealEnemyShips = true;
            refreshSkillShipBox();
            endGame("AI wins. Your fleet is sunk.");
            return;
        }

        String aiSkillMessage = ai.getLastAiSkillMessage();
        String attackMessage = "AI attack flashes: " + hits + " hit, " + misses + " miss.";
        int destroyedDecoys = ai.getLastDestroyedDecoyCount();

        if (destroyedDecoys == 1) {
            attackMessage += "<br>AI destroyed 1 of your Decoy Ship. It was not an objective ship.";
        } else if (destroyedDecoys > 1) {
            attackMessage += "<br>AI destroyed " + destroyedDecoys + " of your Decoy Ships. They were not objective ships.";
        }

        if (aiSkillMessage != null && !aiSkillMessage.isEmpty()) {
            statusLabel.setText("<html>" + aiSkillMessage + "<br>" + attackMessage + "</html>");
        } else {
            statusLabel.setText(attackMessage);
        }

        Timer timer = new Timer(900, e -> {
            ((Timer) e.getSource()).stop();
            player.getBoard().clearRecentAttacks();
            attacksLeft = GameConfig.ATTACKS_PER_TURN;
            playerTurn = true;
            turnLabel.setText("Player Turn - " + attacksLeft + " attacks left");
            statusLabel.setText("Your turn. Attack the AI board.");
            refreshBoards();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void endGame(String message) {
        gameStarted = false;
        playerTurn = false;
        turnLabel.setText("Game Over");
        statusLabel.setText(message);
        refreshBoards();
    }

    private void refreshBoards() {
        playerBoardPanel.refresh();
        enemyBoardPanel.refresh();
    }

    private void refreshSkillShipBox() {
        int selectedIndex = skillShipBox.getSelectedIndex();

        skillShipBox.removeAllItems();

        for (Ship ship : player.getShips()) {
            skillShipBox.addItem(ship.getName() + " - " + ship.getSkillStatusText());
        }

        if (selectedIndex >= 0 && selectedIndex < skillShipBox.getItemCount()) {
            skillShipBox.setSelectedIndex(selectedIndex);
        }
    }
}
