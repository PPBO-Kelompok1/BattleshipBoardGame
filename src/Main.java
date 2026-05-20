import java.util.*;
import java.util.regex.*;

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

    public Ship(String name, int width, int height) {
        this.name = name;
        this.width = width;
        this.height = height;

        direction = Direction.HORIZONTAL;
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

        System.out.println("AI placed " + ship.getName());
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

            System.out.println("AI is now targeting your " + ship.getName() + "!");
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

                            if(nr >= board.getRows() || nc >= board.getCols())
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

            System.out.println("\nAI attacked (" + row + ", " + col + ")");

            if(hit) {

                Ship ship = tile.getShip();

                System.out.println("AI HIT your " + ship.getName() + "!");

                if(target.getBoard().isShipSunk(ship)) {

                    System.out.println("Your " + ship.getName() + " SUNK!");
                }

            } else {

                System.out.println("AI MISSED!");
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

class Game {

    private Player player;
    private AIPlayer ai;

    private Scanner scanner;

    private final int SHIP_COUNT = 3;

    public Game() {

        player = new Player(10, 10);
        ai = new AIPlayer(10, 10);

        scanner = new Scanner(System.in);
    }

    private Difficulty chooseDifficulty() {

        while(true) {

            System.out.println("""
                Choose Difficulty:
                1. Easy
                2. Medium
                3. Hard
                4. Extreme
                """);

            int choice = scanner.nextInt();

            switch(choice) {

                case 1:
                    return Difficulty.EASY;

                case 2:
                    return Difficulty.MEDIUM;

                case 3:
                    return Difficulty.HARD;

                case 4:
                    return Difficulty.EXTREME;

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    public void start() {

        System.out.println(Colors.parse("{FF0000}=== PLAYER SETUP ==="));

        for (int i = 0; i < SHIP_COUNT; i++) {

            Ship ship = chooseShip();

            boolean placed = false;

            while (!placed) {

                System.out.println("Place your " + ship.getName());

                System.out.print("Row: ");
                int row = scanner.nextInt();

                System.out.print("Col: ");
                int col = scanner.nextInt();

                System.out.print("Direction (H/V): ");
                String dir = scanner.next();

                ship.direction = Direction.HORIZONTAL;

                if(dir.equalsIgnoreCase("V")) {
                    ship.rotate();
                }

                placed = player.getBoard().placeShip(ship, row, col);

                if (!placed) {
                    System.out.println("Invalid position!");
                }
            }

            player.addShip(ship);

            player.getBoard().displayPlayerBoard();
        }

        System.out.println("\n=== AI SETUP ===");

        for (int i = 0; i < SHIP_COUNT; i++) {

            Ship ship = ai.randomShip();

            Random rand = new Random();
            boolean randomValue = rand.nextBoolean();

            if(!randomValue) {
                ship.rotate();
            }

            ai.placeShipRandomly(ship);
        }

        System.out.println("\nPLAYER BOARD:");
        player.getBoard().displayPlayerBoard();

        Difficulty diff = chooseDifficulty();

        ai.setDifficulty(diff);

        System.out.println("\n=== GAME START ===");

        while(true) {

            playerTurn();

            if(ai.allShipsSunk()) {

                System.out.println("\nPLAYER WINS!");
                break;
            }

            ai.performTurn(player);

            if(player.allShipsSunk()) {

                System.out.println("\nAI WINS!");
                break;
            }

            System.out.println("\n=== PLAYER BOARD ===");

            player.getBoard().displayPlayerBoard();
        }
    }

    private Ship chooseShip() {

        while (true) {

            System.out.println("""
                    Choose Ship:
                    1. Destroyer (2x1)
                    2. Battleship (2x2)
                    3. Submarine (3x1)
                    """);

            int choice = scanner.nextInt();

            switch(choice) {

                case 1:
                    return new Destroyer();

                case 2:
                    return new Battleship();

                case 3:
                    return new Submarine();

                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    private void playerTurn() {

        System.out.println("\n=== PLAYER TURN ===");

        int attacks = 0;

        while(attacks < 3) {

            System.out.println("\nAI BOARD:");

            ai.getBoard().displayHidden();

            System.out.print("Attack Row: ");
            int row = scanner.nextInt();

            System.out.print("Attack Col: ");
            int col = scanner.nextInt();

            Tile tile = ai.getBoard().getTile(row, col);

            if(tile.isAttacked()) {

                System.out.println("Tile already attacked!");
                continue;
            }

            ai.getBoard().attackTile(row, col);

            if(tile.hasShip()) {

                Ship ship = tile.getShip();

                System.out.println("HIT on an unknown SHIP!");

                if(ai.getBoard().isShipSunk(ship)) {
                    System.out.println("CONGRATS! SUNK on " + ship.getName());
                }

            } else {

                System.out.println("MISS!");
            }

            attacks++;
        }
    }
}

public class Main {

    public static void main(String[] args) {

        Game game = new Game();

        game.start();
    }
}