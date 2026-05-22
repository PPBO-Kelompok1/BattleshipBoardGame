import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

enum Direction {
    HORIZONTAL,
    VERTICAL
}

enum Difficulty {
    EASY,
    MEDIUM,
    HARD,
    EXTREME
}

interface GameCallback {
    void requestCoordinates(String prompt, CoordConsumer consumer);
    void showMessage(String message);
}

interface CoordConsumer {
    void accept(int row, int col);
}

final class Colors {

    public static String parse(String text) {
        StringBuilder out = new StringBuilder();

        for (int i = 0; i < text.length(); ) {

            if (i + 7 < text.length()
                    && text.charAt(i) == '{'
                    && text.charAt(i + 7) == '}') {

                String hex = text.substring(i + 1, i + 7);

                try {
                    int rgb = Integer.parseInt(hex, 16);

                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    out.append("\u001B[38;2;")
                            .append(r).append(";")
                            .append(g).append(";")
                            .append(b).append("m");

                    i += 8;
                    continue;

                } catch (Exception ignored) {}
            }

            out.append(text.charAt(i));
            i++;
        }

        out.append("\u001B[0m");
        return out.toString();
    }
}

abstract class Ship {

    protected String name;
    protected int width;
    protected int height;

    protected int row;
    protected int col;
    protected Direction direction;

    // Atribut tambahan: untuk merge ke visualisasi prototype
    protected int hp;
    protected int maxHp;
    protected boolean skillUsed;

    public Ship(String name, int width, int height, int hp) {
        this.name = name;
        this.width = width;
        this.height = height;

        direction = Direction.HORIZONTAL;

        // Atribut constructor tambahan: untuk merge ke visualisasi prototype
        this.hp = hp;
        this.maxHp = hp;
        this.skillUsed = false;
    }

//    public boolean isSunk() {
//
//    }

    public void place(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public String getName() {
        return name;
    }

    public void rotate() {

        if (direction == Direction.HORIZONTAL) {
            direction = Direction.VERTICAL;
        } else {
            direction = Direction.HORIZONTAL;
        }
    }

    public int getActualWidth() {

        if (direction == Direction.HORIZONTAL) {
            return width;
        }

        return height;
    }

    public int getActualHeight() {

        if (direction == Direction.HORIZONTAL) {
            return height;
        }

        return width;
    }

    public Direction getDirection() {
        return direction;
    }
}

class Destroyer extends Ship {

    public Destroyer() {
        super("Destroyer", 2, 1);
    }
}

class Battleship extends Ship {

    public Battleship() {
        super("Battleship", 2, 2);
    }
}

class Submarine extends Ship {

    public Submarine() {
        super("Submarine", 3, 1);
    }
}

class Tile {

    private Ship ship;
    private boolean attacked;

    private boolean recentlyAttacked;

    public boolean hasShip() {
        return ship != null;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    public Ship getShip() {
        return ship;
    }

    public boolean isAttacked() {
        return attacked;
    }

    public void attack() {
        attacked = true;

//        if(ship != null) {
//            ship.takeDamage();
//        }
    }

    public boolean isRecentlyAttacked() {
        return recentlyAttacked;
    }

    public void setRecentlyAttacked(boolean value) {
        recentlyAttacked = value;
    }
}

class Board {

    private int rows;
    private int cols;

    private Tile[][] grid;

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

    public int getRows(){
        return rows;
    }
    public int getCols(){
        return cols;
    }

    public boolean isShipSunk(Ship ship) {

        for(int r = 0; r < rows; r++) {

            for(int c = 0; c < cols; c++) {

                Tile tile = grid[r][c];

                if(tile.getShip() == ship && !tile.isAttacked()) {
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

    public void displayPlayerBoard() {

        System.out.print("   ");

        for (int c = 0; c < cols; c++) {
            System.out.printf("%2d ", c);
        }

        System.out.println();

        for (int r = 0; r < rows; r++) {

            System.out.printf("%2d ", r);

            for (int c = 0; c < cols; c++) {

                Tile tile = grid[r][c];

                if (tile.isRecentlyAttacked()) {

                    System.out.print(Colors.parse(" {FFFF00}X "));
                }

                else if (tile.isAttacked() && tile.hasShip()) {

                    System.out.print(Colors.parse(" {FF0000}X "));
                }

                else if (tile.isAttacked()) {

                    System.out.print(Colors.parse(" {00FFFF}X "));
                }

                else if (tile.hasShip()) {

                    Ship s = tile.getShip();

                    switch (s) {

                        case Battleship b -> System.out.print(" B ");
                        case Submarine su -> System.out.print(" S ");
                        case Destroyer d -> System.out.print(" D ");

                        default -> System.out.print(" ? ");
                    }
                }

                else {

                    System.out.print(" . ");
                }
            }

            System.out.println();
        }
    }

    public void displayHidden() {

        System.out.print("   ");

        for(int c = 0; c < cols; c++) {
            System.out.printf("%2d ", c);
        }

        System.out.println();

        for(int r = 0; r < rows; r++) {

            System.out.printf("%2d ", r);

            for(int c = 0; c < cols; c++) {

                Tile tile = grid[r][c];

                if(tile.isAttacked()) {

                    if(tile.hasShip()) {
                        System.out.print(" X ");
                    } else {
                        System.out.print(" O ");
                    }

                } else {

                    System.out.print(" . ");
                }
            }

            System.out.println();
        }
    }

    public void clearRecentAttacks() {

        for(int r = 0; r < rows; r++) {

            for(int c = 0; c < cols; c++) {

                grid[r][c].setRecentlyAttacked(false);
            }
        }
    }
}

class AttackMemory {

    int row;
    int col;

    boolean hit;

    int turnNumber;

    public AttackMemory(int row, int col, boolean hit, int turnNumber) {
        this.row = row;
        this.col = col;
        this.hit = hit;
        this.turnNumber = turnNumber;
    }
}


class Player {

    protected Board board;
    protected List<Ship> ships;

    public Player(int rows, int cols) {

        board = new Board(rows, cols);
        ships = new ArrayList<>();
    }

    public Board getBoard() {
        return board;
    }

    public void addShip(Ship ship) {
        ships.add(ship);
    }

    public boolean allShipsSunk() {

        for(Ship ship : ships) {

            if(!board.isShipSunk(ship)) {
                return false;
            }
        }

        return true;
    }
}
class HitCluster {

    List<int[]> hits;

    public HitCluster() {
        hits = new ArrayList<>();
    }

    public void add(int row, int col) {
        hits.add(new int[]{row, col});
    }

    public List<int[]> getHits() {
        return hits;
    }
}

class AIPlayer extends Player {

    private Random random;
    private List<Ship> targetShips;

    private List<AttackMemory> memory;
    private Difficulty difficulty;
    private int currentTurn;

    public AIPlayer(int rows, int cols) {
        super(rows, cols);
        random = new Random();

        memory = new ArrayList<>();
        targetShips = new ArrayList<>();
        currentTurn = 0;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Ship randomShip() {

        int choice = random.nextInt(3);

        switch(choice) {
            case 0:
                return new Destroyer();

            case 1:
                return new Battleship();

            default:
                return new Submarine();
        }
    }

    public void placeShipRandomly(Ship ship) {

        boolean placed = false;

        while (!placed) {

            int row = random.nextInt(board.getRows());
            int col = random.nextInt(board.getCols());

            placed = board.placeShip(ship, row, col);
        }

        addShip(ship);

    }
    public void processMemoryForget() {

        Iterator<AttackMemory> iterator = memory.iterator();

        while(iterator.hasNext()) {

            AttackMemory mem = iterator.next();

            int age = currentTurn - mem.turnNumber;

            int forgetChance = 0;

            switch(difficulty) {

                case EASY -> {

                    if(age >= 2 && age <= 3)
                        forgetChance = 30;

                    else if(age >= 4 && age <= 5)
                        forgetChance = 35;

                    else if(age >= 6)
                        forgetChance = 40;
                }

                case MEDIUM -> {

                    if(age >= 3 && age <= 4)
                        forgetChance = 20;

                    else if(age >= 5 && age <= 6)
                        forgetChance = 25;

                    else if(age >= 7)
                        forgetChance = 30;
                }

                case HARD -> {

                    if(mem.hit)
                        continue;

                    if(age >= 3 && age <= 4)
                        forgetChance = 10;

                    else if(age >= 5 && age <= 6)
                        forgetChance = 15;

                    else if(age >= 7)
                        forgetChance = 20;
                }

                case EXTREME -> {
                    continue;
                }
            }

            int roll = random.nextInt(100);

            if(roll < forgetChance) {
                iterator.remove();
            }
        }
    }

    private boolean alreadyRemembered(int row, int col) {

        for(AttackMemory mem : memory) {

            if(mem.row == row && mem.col == col) {
                return true;
            }
        }

        return false;
    }

    private void discoverShip(Ship ship, Board board) {

        if(difficulty == Difficulty.EASY)
            return;

        if(board.isShipSunk(ship))
            return;

        if(!targetShips.contains(ship)) {

            targetShips.add(ship);

        }
    }

    private int[] getFocusedAttack(Player target) {

        Board board = target.getBoard();

        Iterator<Ship> iterator = targetShips.iterator();

        while(iterator.hasNext()) {

            Ship ship = iterator.next();

            if(board.isShipSunk(ship)) {
                iterator.remove();
                continue;
            }

            for(int r = 0; r < board.getRows(); r++) {

                for(int c = 0; c < board.getCols(); c++) {

                    Tile tile = board.getTile(r, c);

                    if(tile.getShip() == ship && tile.isAttacked()) {

                        int[][] dirs = {
                                {-1,0},
                                {1,0},
                                {0,-1},
                                {0,1}
                        };

                        for(int[] d : dirs) {

                            int nr = r + d[0];
                            int nc = c + d[1];

                            if(nr < 0 || nc < 0 || nr >= board.getRows() || nc >= board.getCols())
                                continue;

                            if(alreadyRemembered(nr, nc))
                                continue;

                            return new int[]{nr, nc};
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isValidProbabilityTile(Tile tile) {
        return !tile.isAttacked();
    }

    private int[][] generateHeatmap(Player target) {
        Board board = target.getBoard();

        int[][] heatmap = new int[board.getRows()][board.getCols()];
        List<HitCluster> clusters = getHitClusters(target);

        // try every ship type
        List<Ship> possibleShips = List.of(
                new Destroyer(),
                new Battleship(),
                new Submarine()
        );

        for(Ship ship : possibleShips) {
            for(Direction dir : Direction.values()) {

                if(dir == Direction.VERTICAL) {
                    ship.rotate();
                }

                // try every board pos
                for(int row = 0; row < board.getRows(); row++) {

                    for(int col = 0; col < board.getCols(); col++) {

                        boolean valid = true;

                        // check placement validity
                        for(int r = 0; r < ship.getActualHeight(); r++) {

                            for(int c = 0; c < ship.getActualWidth(); c++) {

                                int nr = row + r;
                                int nc = col + c;

                                // out of zone/bounds
                                if(nr >= board.getRows() || nc >= board.getCols()){
                                    valid = false;
                                    break;
                                }

                                Tile tile = board.getTile(nr, nc);

                                // no place on missed attack
                                if(tile.isAttacked() && !tile.hasShip()) {
                                    valid = false;
                                    break;
                                }
                            }

                            if(!valid)
                                break;
                        }

                        // valid tile/placement = add heat
                        int clusterMatches = 0;

                        for(HitCluster cluster : clusters) {

                            boolean clusterCovered = false;

                            for(int[] hit : cluster.getHits()) {

                                int hr = hit[0];
                                int hc = hit[1];

                                for(int r = 0; r < ship.getActualHeight(); r++) {

                                    for(int c = 0; c < ship.getActualWidth(); c++) {

                                        int nr = row + r;
                                        int nc = col + c;

                                        if(nr == hr && nc == hc) {

                                            clusterCovered = true;
                                            break;
                                        }
                                    }

                                    if(clusterCovered)
                                        break;
                                }

                                if(clusterCovered)
                                    break;
                            }

                            if(clusterCovered) {
                                clusterMatches++;
                            }
                        }

                        if(valid) {

                            for(int r = 0; r < ship.getActualHeight(); r++) {

                                for(int c = 0; c < ship.getActualWidth(); c++) {

                                    int nr = row + r;
                                    int nc = col + c;

                                    if(!board.getTile(nr, nc).isAttacked()) {

                                        heatmap[nr][nc] += 1 + (clusterMatches * 5); // change to around *15 for MORE agggressive/fixated AI
                                    }
                                }
                            }
                        }
                    }
                }

                // orientation restore
                if(dir == Direction.VERTICAL) {
                    ship.rotate();
                }
            }
        }

        return heatmap;
    }

    private int[] getBestProbabilityAttack(Player target) {

        int[][] heatmap = generateHeatmap(target);

        int bestScore = -1;

        List<int[]> bestTiles = new ArrayList<>();

        for(int r = 0; r < board.getRows(); r++) {

            for(int c = 0; c < board.getCols(); c++) {

                if(alreadyRemembered(r, c))
                    continue;

                int score = heatmap[r][c];

                if(score > bestScore) {

                    bestScore = score;

                    bestTiles.clear();

                    bestTiles.add(new int[]{r, c});
                }

                else if(score == bestScore) {

                    bestTiles.add(new int[]{r, c});
                }
            }
        }

        return bestTiles.get(random.nextInt(bestTiles.size()));
    }

    public void performTurn(Player target) {

        currentTurn++;

        processMemoryForget();

        target.getBoard().clearRecentAttacks();

        int attacks = 0;

        while(attacks < 3) {

            int row;
            int col;

            int[] focusedAttack = null;

            if(difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) {

                focusedAttack = getFocusedAttack(target);
            }

            // target ship mode
            if(focusedAttack != null) {

                row = focusedAttack[0];
                col = focusedAttack[1];

            }

            // random/search mode
            else {
                if(difficulty == Difficulty.EXTREME) {

                    int[] move = getBestProbabilityAttack(target);

                    row = move[0];
                    col = move[1];
                }
                else {
                    if(difficulty != Difficulty.EASY) {
                        focusedAttack = getFocusedAttack(target);
                    }

                    if(focusedAttack != null) {

                        row = focusedAttack[0];
                        col = focusedAttack[1];

                    } else {

                        row = random.nextInt(board.getRows());
                        col = random.nextInt(board.getCols());

                        if(alreadyRemembered(row, col)) {
                            continue;
                        }
                    }
                }
            }

            Tile tile = target.getBoard().getTile(row, col);

            target.getBoard().attackTile(row, col);

            tile.setRecentlyAttacked(true);

            boolean hit = tile.hasShip();

            if(hit) {

                Ship ship = tile.getShip();

                discoverShip(ship, target.getBoard());
            }

            memory.add(new AttackMemory(row, col, hit, currentTurn));

            if(hit) {

                Ship ship = tile.getShip();

                if(target.getBoard().isShipSunk(ship)) {
                }

            } else {
            }

            attacks++;
        }
    }

    private void floodFillCluster(
            Board board,
            boolean[][] visited,
            HitCluster cluster,
            int row,
            int col
    ) {

        if(row < 0 || row >= board.getRows() || col < 0 || col >= board.getCols())
            return;

        if(visited[row][col])
            return;

        Tile tile = board.getTile(row, col);

        if(!(tile.isAttacked() && tile.hasShip()))
            return;

        visited[row][col] = true;

        cluster.add(row, col);

        int[][] dirs = {
                {-1,0},
                {1,0},
                {0,-1},
                {0,1}
        };

        for(int[] d : dirs) {

            floodFillCluster(
                    board,
                    visited,
                    cluster,
                    row + d[0],
                    col + d[1]
            );
        }
    }

    private List<HitCluster> getHitClusters(Player target) {

        List<HitCluster> clusters = new ArrayList<>();

        boolean[][] visited = new boolean[board.getRows()][board.getCols()];

        Board board = target.getBoard();

        for(int r = 0; r < board.getRows(); r++) {

            for(int c = 0; c < board.getCols(); c++) {

                Tile tile = board.getTile(r, c);

                if(visited[r][c])
                    continue;

                if(!(tile.isAttacked() && tile.hasShip()))
                    continue;

                HitCluster cluster = new HitCluster();

                floodFillCluster(board, visited, cluster, r, c);

                clusters.add(cluster);
            }
        }

        return clusters;
    }
}

class Game extends JFrame {

    private static final int BOARD_SIZE = 10;
    private static final int SHIP_COUNT = 3;
    private static final int ATTACKS_PER_TURN = 3;

    private final Player player;
    private final AIPlayer ai;
    private final BoardPanel playerBoardPanel;
    private final BoardPanel enemyBoardPanel;
    private final JComboBox<Difficulty> difficultyBox;
    private final JComboBox<String> shipBox;
    private final JComboBox<Direction> directionBox;
    private final JLabel statusLabel;
    private final JLabel turnLabel;
    private final JLabel setupLabel;

    private int placedShips;
    private int attacksLeft;
    private boolean gameStarted;
    private boolean playerTurn;
    private boolean revealEnemyShips;

    public Game() {
        super("Battleship Swing");

        player = new Player(BOARD_SIZE, BOARD_SIZE);
        ai = new AIPlayer(BOARD_SIZE, BOARD_SIZE);

        difficultyBox = new JComboBox<>(Difficulty.values());
        shipBox = new JComboBox<>(new String[]{
                "Destroyer (2x1)",
                "Battleship (2x2)",
                "Submarine (3x1)"
        });
        directionBox = new JComboBox<>(Direction.values());
        statusLabel = new JLabel("Choose difficulty, then place 3 ships on your board.");
        turnLabel = new JLabel("Setup");
        setupLabel = new JLabel("Ships placed: 0 / " + SHIP_COUNT);

        playerBoardPanel = new BoardPanel(player.getBoard(), true);
        enemyBoardPanel = new BoardPanel(ai.getBoard(), false);

        difficultyBox.setRenderer(new FriendlyEnumRenderer<>());
        directionBox.setRenderer(new FriendlyEnumRenderer<>());

        buildUi();
        refreshBoards();
    }

    public void start() {
        setVisible(true);
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);

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
            return;
        }

        Ship ship = createSelectedShip();
        ship.direction = (Direction) directionBox.getSelectedItem();

        if (!player.getBoard().placeShip(ship, row, col)) {
            statusLabel.setText("Invalid placement. Ships cannot overlap or leave the board.");
            return;
        }

        player.addShip(ship);
        placedShips++;
        setupLabel.setText("Ships placed: " + placedShips + " / " + SHIP_COUNT);
        statusLabel.setText(ship.getName() + " placed. Choose the next ship and tile.");

        if (placedShips == SHIP_COUNT) {
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
        attacksLeft = ATTACKS_PER_TURN;
        turnLabel.setText("Player Turn");
        setupLabel.setText("Attack the concealed AI board.");
        statusLabel.setText("Game start. You have " + attacksLeft + " attacks.");
    }

    private void placeAiShips() {
        Random random = new Random();

        for (int i = 0; i < SHIP_COUNT; i++) {
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
            default -> new Destroyer();
        };
    }

    private void handleEnemyBoardClick(int row, int col) {
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
                statusLabel.setText("Red explosion. You sunk an enemy " + ship.getName() + "!");
            } else {
                statusLabel.setText("Red explosion. Hit on an enemy ship.");
            }
        } else {
            statusLabel.setText("Miss. " + attacksLeft + " attacks left.");
        }

        refreshBoards();

        if (ai.allShipsSunk()) {
            endGame("Player wins. All AI ships are sunk.");
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

        javax.swing.Timer timer = new javax.swing.Timer(650, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            ai.performTurn(player);
            showAiAttackResult();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void showAiAttackResult() {
        int hits = 0;
        int misses = 0;

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
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

        if (player.allShipsSunk()) {
            revealEnemyShips = true;
            endGame("AI wins. Your fleet is sunk.");
            return;
        }

        statusLabel.setText("AI attack flashes: " + hits + " hit, " + misses + " miss.");

        javax.swing.Timer timer = new javax.swing.Timer(900, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            player.getBoard().clearRecentAttacks();
            attacksLeft = ATTACKS_PER_TURN;
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

    private class BoardPanel extends JPanel {

        private final Board board;
        private final boolean playerBoard;
        private final TileButton[][] buttons;

        BoardPanel(Board board, boolean playerBoard) {
            super(new GridLayout(BOARD_SIZE, BOARD_SIZE, 2, 2));
            this.board = board;
            this.playerBoard = playerBoard;
            buttons = new TileButton[BOARD_SIZE][BOARD_SIZE];

            setBackground(new Color(7, 18, 30));
            setBorder(new EmptyBorder(8, 8, 8, 8));

            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    TileButton button = new TileButton(r, c);
                    buttons[r][c] = button;
                    add(button);
                }
            }
        }

        void refresh() {
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    styleButton(buttons[r][c], false);
                }
            }
        }

        private void showPlacementHover(int row, int col, boolean show) {
            if (!playerBoard || gameStarted) {
                return;
            }

            refresh();

            if (!show) {
                return;
            }

            Ship ship = createSelectedShip();
            ship.direction = (Direction) directionBox.getSelectedItem();
            boolean valid = board.canPlaceShip(ship, row, col);

            for (int r = 0; r < ship.getActualHeight(); r++) {
                for (int c = 0; c < ship.getActualWidth(); c++) {
                    int nr = row + r;
                    int nc = col + c;

                    if (nr >= 0 && nr < BOARD_SIZE && nc >= 0 && nc < BOARD_SIZE) {
                        buttons[nr][nc].setBackground(valid ? new Color(73, 187, 119) : new Color(184, 50, 59));
                    }
                }
            }
        }

        private void showTargetHover(TileButton button, boolean show) {
            if (playerBoard || !gameStarted || !playerTurn) {
                return;
            }

            styleButton(button, show && !board.getTile(button.row, button.col).isAttacked());
        }

        private void styleButton(TileButton button, boolean hoverTarget) {
            Tile tile = board.getTile(button.row, button.col);
            button.setText("");
            button.setForeground(Color.WHITE);

            if (tile.isRecentlyAttacked()) {
                button.setBackground(new Color(255, 194, 41));
                button.setText(tile.hasShip() ? "X" : "O");
                return;
            }

            if (tile.isAttacked() && tile.hasShip()) {
                button.setBackground(new Color(202, 35, 45));
                button.setText("X");
                return;
            }

            if (tile.isAttacked()) {
                button.setBackground(new Color(194, 232, 240));
                button.setForeground(new Color(23, 55, 73));
                button.setText("O");
                return;
            }

            if ((playerBoard || revealEnemyShips) && tile.hasShip()) {
                button.setBackground(new Color(91, 106, 117));
                button.setText(shipCode(tile.getShip()));
                return;
            }

            button.setBackground(hoverTarget ? new Color(84, 181, 219) : new Color(20, 117, 171));
        }

        private String shipCode(Ship ship) {
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

        private class TileButton extends JButton {

            private final int row;
            private final int col;

            TileButton(int row, int col) {
                this.row = row;
                this.col = col;

                setPreferredSize(new Dimension(42, 42));
                setFocusPainted(false);
                setBorderPainted(false);
                setOpaque(true);
                setFont(getFont().deriveFont(Font.BOLD, 16f));
                setToolTipText(row + ", " + col);
                setAlignmentX(Component.CENTER_ALIGNMENT);

                addActionListener(e -> {
                    if (playerBoard) {
                        handlePlayerBoardClick(row, col);
                    } else {
                        handleEnemyBoardClick(row, col);
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

    private static class FriendlyEnumRenderer<T extends Enum<T>> extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Enum<?> enumValue) {
                String text = enumValue.name().toLowerCase(Locale.ROOT).replace('_', ' ');
                setText(Character.toUpperCase(text.charAt(0)) + text.substring(1));
            }

            return this;
        }
    }
}

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new Game().start());
    }
}
