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
    protected int hp;
    protected int maxHp;
    protected Direction direction;

    protected boolean skillUsed;

    public Ship(String name, int width, int height, int hp) {
        this.name = name;
        this.width = width;
        this.height = height;

        this.hp = hp;
        this.maxHp = hp;

        direction = Direction.HORIZONTAL;

        skillUsed = false;
    }

    public void takeDamage() {
        hp--;
    }

    public boolean isSunk() {
        return hp <= 0;
    }

    public int getHp() {
        return hp;
    }

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

    public boolean isSkillUsed() {
        return skillUsed;
    }

    public void resetSkill() {
        skillUsed = false;
    }

    public abstract void useSkill(Board enemyBoard, Scanner scanner);
}

class Destroyer extends Ship {

    public Destroyer() {
        super("Destroyer", 2, 1, 1);
    }

    @Override
    public void useSkill(Board enemyBoard, Scanner scanner) {

        if(skillUsed) {
            System.out.println("Destroyer skill already used!");
            return;
        }

        System.out.println("\n=== DESTTROYER SKILL: DOUBLE STRIKE ===");

        for(int i = 1; i <= 2; i++) {
            System.out.println("Attack #" + i);

            System.out.println("Row: ");
            int row = scanner.nextInt();

            System.out.println("Col: ");
            int col = scanner.nextInt();

            Tile tile = enemyBoard.getTile(row, col);

            if(tile.isAttacked()) {
                System.out.println("Tile already attacked!");
                i--;
                continue;
            }

            enemyBoard.attackTile(row, col);

            if(tile.hasShip()) {
                Ship ship = tile.getShip();

                System.out.println("HIT on " + ship.getName());

                if(ship.isSunk()) {
                    System.out.println(ship.getName() + " SUNK!");
                }
            } else {
                System.out.println("MISS!");
            }
        }

        skillUsed = true;
    }
}

class Battleship extends Ship {

    public Battleship() {
        super("Battleship", 2, 2, 1);
    }
}

class Submarine extends Ship {

    public Submarine() {
        super("Submarine", 3, 1, 1);
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

        if(ship != null) {
            ship.takeDamage();
        }
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

            if(!ship.isSunk()) {
                return false;
            }
        }

        return true;
    }
}

class AIPlayer extends Player {

    private Random random;

    private List<AttackMemory> memory;
    private Difficulty difficulty;
    private int currentTurn;

    public AIPlayer(int rows, int cols) {
        super(rows, cols);
        random = new Random();

        memory = new ArrayList<>();
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

            int row = random.nextInt(10);
            int col = random.nextInt(10);

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

    public void performTurn(Player target) {
        target.getBoard().clearRecentAttacks();

        currentTurn++;

        processMemoryForget();

        int attacks = 0;

        while(attacks < 3) {

            int row = random.nextInt(10);
            int col = random.nextInt(10);

            if(alreadyRemembered(row, col)) {
                continue;
            }

            Tile tile = target.getBoard().getTile(row, col);

            tile.setRecentlyAttacked(true);

            target.getBoard().attackTile(row, col);

            boolean hit = tile.hasShip();

            memory.add(new AttackMemory(row, col, hit, currentTurn));

            System.out.println("\nAI attacked (" + row + ", " + col + ")");

            if(hit) {

                Ship ship = tile.getShip();

                System.out.println("AI HIT your " + ship.getName() + "!");

                if(ship.isSunk()) {
                    System.out.println("Your " + ship.getName() + " SUNK!");
                }

            } else {

                System.out.println("AI MISSED!");
            }

            attacks++;
        }
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

                if (dir.equalsIgnoreCase("V")) {
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

                System.out.println("HIT on " + ship.getName() + "!");

                if(ship.isSunk()) {
                    System.out.println(ship.getName() + " SUNK!");
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