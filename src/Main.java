import java.util.*;

enum Direction {
    HORIZONTAL,
    VERTICAL
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

    public boolean hasShip() {
        return ship != null;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    public Ship getShip() {
        return ship;
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

    public void display() {

        for (int r = 0; r < rows; r++) {

            for (int c = 0; c < cols; c++) {

                if (grid[r][c].hasShip()) {

                    Ship s = grid[r][c].getShip();

                    switch(s){
                        case Battleship b -> System.out.print(" B ");
                        case Submarine su -> System.out.print(" S ");
                        case Destroyer d -> System.out.print(" D ");
                        case null -> System.out.print(" . ");
                        default -> System.out.print(" . ");
                    }
                    //System.out.print(" S ");
                } else {
                    System.out.print(" . ");
                }
            }

            System.out.println();
        }
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
}

class AIPlayer extends Player {

    private Random random;

    public AIPlayer(int rows, int cols) {
        super(rows, cols);
        random = new Random();
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

    public void start() {

        System.out.println("=== PLAYER SETUP ===");

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

            player.getBoard().display();
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
        player.getBoard().display();

        System.out.println("\nAI BOARD:");
        ai.getBoard().display();
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
}

public class Main {

    public static void main(String[] args) {

        Game game = new Game();

        sc.close();

        // tes push: Angga
    }
}