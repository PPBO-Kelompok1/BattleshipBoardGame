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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.ParseException;

public class Game extends JFrame implements GameCallback {

    private static final int AI_SHIP_CREATION_ATTEMPTS = 100;

    private final GameConfig config;
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
    private JButton skillButton;
    private JButton rotateButton;

    private int placedShips;
    private int attacksLeft;
    private boolean gameStarted;
    private boolean gameOver;
    private boolean playerTurn;
    private boolean revealEnemyShips;
    private boolean awaitingSkillInput;
    private CoordTarget pendingCoordTarget;
    private CoordConsumer pendingCoordConsumer;

    public Game() {
        super("Battleship Swing");

        config = GameConfig.getInstance().copy();
        player = new Player(config.getBoardSize(), config.getBoardSize());
        ai = new AIPlayer(config.getBoardSize(), config.getBoardSize(), config);

        difficultyBox = new JComboBox<>(Difficulty.values());
        difficultyBox.setSelectedItem(config.getDifficulty());
        difficultyBox.addActionListener(e -> syncDifficultyFromLegacySelector());

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
        statusLabel = new JLabel("Choose difficulty, then place " + config.getShipsPerType() + " ships on your board.");
        turnLabel = new JLabel("Setup");
        setupLabel = new JLabel("Ships placed: 0 / " + config.getShipsPerType());

        playerBoardPanel = new BoardPanel(
                player.getBoard(),
                true,
                () -> gameStarted,
                () -> gameOver,
                () -> playerTurn,
                () -> revealEnemyShips,
                this::createSelectedShip,
                () -> (Direction) directionBox.getSelectedItem(),
                this::handlePlayerBoardClick
        );
        enemyBoardPanel = new BoardPanel(
                ai.getBoard(),
                false,
                () -> gameStarted,
                () -> gameOver,
                () -> playerTurn,
                () -> revealEnemyShips,
                this::createSelectedShip,
                () -> (Direction) directionBox.getSelectedItem(),
                this::handleEnemyBoardClick
        );

        difficultyBox.setRenderer(new FriendlyEnumRenderer<>());
        directionBox.setRenderer(new FriendlyEnumRenderer<>());

        buildUi();
        updateControlsEnabled();
        refreshBoards();
    }

    @Override
    public void requestCoordinates(String prompt, CoordConsumer consumer, CoordTarget target) {
        if (gameOver) {
            return;
        }

        if (target == CoordTarget.ENEMY_BOARD && !canUseAttack()) {
            statusLabel.setText("No attacks left this round.");
            return;
        }

        awaitingSkillInput = true;
        pendingCoordTarget = target;
        pendingCoordConsumer = consumer;
        String boardName = target == CoordTarget.OWN_BOARD ? "your board" : "the AI board";
        statusLabel.setText(prompt + " - click a tile on " + boardName + ".");
    }

    @Override
    public void showMessage(String message) {
        if (!gameOver) {
            statusLabel.setText("<html>" + message + "</html>");
        }
    }

    @Override
    public int getAttacksLeft() {
        return attacksLeft;
    }

    @Override
    public boolean canUseAttack() {
        return gameStarted && !gameOver && playerTurn && attacksLeft > 0;
    }

    @Override
    public boolean consumeAttack() {
        if (!canUseAttack()) {
            return false;
        }

        attacksLeft--;
        refreshBoards();
        checkWinCondition();

        if (gameOver) {
            clearPendingSkillInput();
            return false;
        }

        if (attacksLeft <= 0) {
            clearPendingSkillInput();
            startAiTurn();
            return false;
        }

        turnLabel.setText("Player Turn - " + attacksLeft + " attacks left");
        return true;
    }

    @Override
    public boolean isGameOver() {
        return gameOver;
    }

    public void start() {
        setVisible(true);
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);

        skillButton = new JButton("Use Skill");
        skillButton.addActionListener(e -> useSelectedSkill());

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

        rotateButton = new JButton("Rotate");
        rotateButton.addActionListener(e -> {
            if (!gameOver && !gameStarted) {
                toggleDirection();
            }
        });
        controls.add(rotateButton);

        JButton configButton = new JButton("Game Config");
        configButton.addActionListener(e -> showConfigDialog());
        controls.add(configButton);

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

    private void showConfigDialog() {
        GameConfig pendingConfig = GameConfig.getInstance();
        JDialog dialog = new JDialog(this, "Game Config", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JComboBox<Difficulty> difficultySelector = new JComboBox<>(Difficulty.values());
        difficultySelector.setRenderer(new FriendlyEnumRenderer<>());
        difficultySelector.setSelectedItem(pendingConfig.getDifficulty());

        JSpinner attacksSpinner = new JSpinner(new SpinnerNumberModel(
                pendingConfig.getAttacksPerRound(),
                GameConfig.MIN_ATTACKS_PER_ROUND,
                GameConfig.MAX_ATTACKS_PER_ROUND,
                1
        ));
        JSpinner shipsSpinner = new JSpinner(new SpinnerNumberModel(
                pendingConfig.getShipsPerType(),
                GameConfig.MIN_SHIPS_PER_TYPE,
                GameConfig.MAX_SHIPS_PER_TYPE,
                1
        ));
        JSpinner boardSizeSpinner = new JSpinner(new SpinnerNumberModel(
                pendingConfig.getBoardSize(),
                GameConfig.MIN_BOARD_SIZE,
                GameConfig.MAX_BOARD_SIZE,
                1
        ));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.setBorder(new EmptyBorder(14, 14, 10, 14));
        form.add(new JLabel("Difficulty"));
        form.add(difficultySelector);
        form.add(new JLabel("Attacks per round"));
        form.add(attacksSpinner);
        form.add(new JLabel("Ships to place"));
        form.add(shipsSpinner);
        form.add(new JLabel("Board size"));
        form.add(boardSizeSpinner);

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            savePendingConfig(difficultySelector, attacksSpinner, shipsSpinner, boardSizeSpinner);
            statusLabel.setText(configChangeMessage());
        });

        JButton resetButton = new JButton("Reset Defaults");
        resetButton.addActionListener(e -> {
            pendingConfig.resetToDefaults();
            difficultySelector.setSelectedItem(pendingConfig.getDifficulty());
            attacksSpinner.setValue(pendingConfig.getAttacksPerRound());
            shipsSpinner.setValue(pendingConfig.getShipsPerType());
            boardSizeSpinner.setValue(pendingConfig.getBoardSize());
            syncLegacyDifficultySelector(pendingConfig.getDifficulty());
            statusLabel.setText(configChangeMessage());
        });

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel();
        buttons.add(applyButton);
        buttons.add(resetButton);
        buttons.add(closeButton);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void savePendingConfig(
            JComboBox<Difficulty> difficultySelector,
            JSpinner attacksSpinner,
            JSpinner shipsSpinner,
            JSpinner boardSizeSpinner
    ) {
        GameConfig pendingConfig = GameConfig.getInstance();
        pendingConfig.setDifficulty((Difficulty) difficultySelector.getSelectedItem());
        pendingConfig.setAttacksPerRound(spinnerValue(attacksSpinner));
        pendingConfig.setShipsPerType(spinnerValue(shipsSpinner));
        pendingConfig.setBoardSize(spinnerValue(boardSizeSpinner));
        syncLegacyDifficultySelector(pendingConfig.getDifficulty());
    }

    private int spinnerValue(JSpinner spinner) {
        try {
            spinner.commitEdit();
        } catch (ParseException ignored) {
            spinner.setValue(((SpinnerNumberModel) spinner.getModel()).getValue());
        }

        Object value = spinner.getValue();

        if (value instanceof Number number) {
            return number.intValue();
        }

        return ((Number) ((SpinnerNumberModel) spinner.getModel()).getValue()).intValue();
    }

    private String configChangeMessage() {
        if (gameStarted || placedShips > 0) {
            return "Config saved. Changes apply to the next New Game.";
        }

        return "Config saved. Press New Game to rebuild the board with these settings.";
    }

    private void syncDifficultyFromLegacySelector() {
        Difficulty selected = (Difficulty) difficultyBox.getSelectedItem();
        GameConfig.getInstance().setDifficulty(selected);

        if (!gameStarted && !gameOver) {
            config.setDifficulty(selected);
        }
    }

    private void syncLegacyDifficultySelector(Difficulty difficulty) {
        difficultyBox.setSelectedItem(difficulty);
    }

    private void toggleDirection() {
        Direction selected = (Direction) directionBox.getSelectedItem();
        directionBox.setSelectedItem(selected == Direction.HORIZONTAL ? Direction.VERTICAL : Direction.HORIZONTAL);
    }

    private void restart() {
        dispose();
        SwingUtilities.invokeLater(() -> new Game().start());
    }

    private void useSelectedSkill() {
        if (gameOver || !gameStarted || !playerTurn || awaitingSkillInput) {
            return;
        }

        int idx = skillShipBox.getSelectedIndex();

        if (idx < 0 || idx >= player.getShips().size()) {
            return;
        }

        Ship ship = player.getShips().get(idx);
        ship.useSkill(player.getBoard(), ai.getBoard(), this);
        refreshSkillShipBox();
        refreshBoards();
    }

    private void handlePlayerBoardClick(int row, int col) {
        if (gameOver) {
            return;
        }

        if (gameStarted) {
            if (awaitingSkillInput && pendingCoordTarget == CoordTarget.OWN_BOARD && pendingCoordConsumer != null) {
                CoordConsumer consumer = consumePendingCoordConsumer();
                consumer.accept(row, col);
                refreshSkillShipBox();
                refreshBoards();
                updateControlsEnabled();
                return;
            }

            if (awaitingSkillInput) {
                statusLabel.setText("That skill is waiting for a target on the AI board.");
            }

            return;
        }

        if (placedShips >= config.getShipsPerType()) {
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
        setupLabel.setText("Ships placed: " + placedShips + " / " + config.getShipsPerType());
        statusLabel.setText(ship.getName() + " placed. Choose the next ship and tile.");

        if (placedShips == config.getShipsPerType()) {
            finishSetup();
        }

        refreshBoards();
        updateControlsEnabled();
    }

    private void finishSetup() {
        if (gameOver) {
            return;
        }

        ai.setDifficulty((Difficulty) difficultyBox.getSelectedItem());

        if (!placeAiShips()) {
            gameOver = true;
            turnLabel.setText("Setup Failed");
            setupLabel.setText("AI fleet could not be placed.");
            statusLabel.setText("Could not place the AI fleet with this configuration. Press New Game after changing config.");
            updateControlsEnabled();
            refreshBoards();
            return;
        }

        gameStarted = true;
        playerTurn = true;
        attacksLeft = config.getAttacksPerRound();
        turnLabel.setText("Player Turn - " + attacksLeft + " attacks left");
        setupLabel.setText("Attack the concealed AI board.");
        statusLabel.setText("Game start. You have " + attacksLeft + " attacks.");
        refreshSkillShipBox();
        updateControlsEnabled();
    }

    private boolean placeAiShips() {
        for (int i = 0; i < config.getShipsPerType(); i++) {
            boolean placed = false;

            for (int attempt = 0; attempt < AI_SHIP_CREATION_ATTEMPTS && !placed; attempt++) {
                placed = ai.placeShipRandomly(ai.randomShip());
            }

            if (!placed) {
                return false;
            }
        }

        return true;
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
        if (gameOver) {
            return;
        }

        if (ai.allShipsSunk()) {
            revealEnemyShips = true;
            endGame("Player wins. All AI ships are sunk.");
        }
    }

    private void handleEnemyBoardClick(int row, int col) {
        if (gameOver) {
            return;
        }

        if (awaitingSkillInput && pendingCoordTarget == CoordTarget.ENEMY_BOARD && pendingCoordConsumer != null) {
            CoordConsumer consumer = consumePendingCoordConsumer();
            consumer.accept(row, col);
            refreshSkillShipBox();
            refreshBoards();
            updateControlsEnabled();
            return;
        }

        if (awaitingSkillInput) {
            statusLabel.setText("That skill is waiting for a target on your board.");
            return;
        }

        if (!canUseAttack()) {
            return;
        }

        Tile tile = ai.getBoard().getTile(row, col);

        if (tile.isAttacked()) {
            statusLabel.setText("That tile was already attacked. Pick another target.");
            return;
        }

        ai.getBoard().attackTile(row, col);

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
            statusLabel.setText("Miss.");
        }

        consumeAttack();
        refreshBoards();
        updateControlsEnabled();
    }

    private CoordConsumer consumePendingCoordConsumer() {
        awaitingSkillInput = false;
        pendingCoordTarget = null;
        CoordConsumer consumer = pendingCoordConsumer;
        pendingCoordConsumer = null;
        return consumer;
    }

    private void clearPendingSkillInput() {
        awaitingSkillInput = false;
        pendingCoordTarget = null;
        pendingCoordConsumer = null;
    }

    private void startAiTurn() {
        if (gameOver) {
            return;
        }

        playerTurn = false;
        turnLabel.setText("AI Turn");
        statusLabel.setText("AI is targeting your board...");
        updateControlsEnabled();

        Timer timer = new Timer(650, e -> {
            ((Timer) e.getSource()).stop();

            if (gameOver) {
                return;
            }

            ai.performTurn(player);
            showAiAttackResult();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showAiAttackResult() {
        if (gameOver) {
            return;
        }

        int hits = 0;
        int misses = 0;

        for (int r = 0; r < player.getBoard().getRows(); r++) {
            for (int c = 0; c < player.getBoard().getCols(); c++) {
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

            if (gameOver) {
                return;
            }

            player.getBoard().clearRecentAttacks();
            attacksLeft = config.getAttacksPerRound();
            playerTurn = true;
            turnLabel.setText("Player Turn - " + attacksLeft + " attacks left");
            statusLabel.setText("Your turn. Attack the AI board.");
            refreshBoards();
            updateControlsEnabled();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void endGame(String message) {
        gameOver = true;
        gameStarted = false;
        playerTurn = false;
        clearPendingSkillInput();
        turnLabel.setText("Game Over");
        statusLabel.setText(message);
        updateControlsEnabled();
        refreshBoards();
    }

    private void updateControlsEnabled() {
        boolean setupOpen = !gameStarted && !gameOver;
        difficultyBox.setEnabled(setupOpen);
        shipBox.setEnabled(setupOpen);
        directionBox.setEnabled(setupOpen);
        rotateButton.setEnabled(setupOpen);
        skillShipBox.setEnabled(gameStarted && playerTurn && !gameOver);
        skillButton.setEnabled(gameStarted && playerTurn && !gameOver);
        playerBoardPanel.setBoardInteractionEnabled(!gameOver);
        enemyBoardPanel.setBoardInteractionEnabled(!gameOver);
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
