package systems.ai;

import core.Difficulty;
import core.Direction;
import entities.AIPlayer;
import entities.Player;
import entities.Ship;
import physics.Board;
import physics.Tile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AttackPlanner {

    private final Random random;
    private final List<Ship> targetShips;
    private final List<AttackMemory> memory;
    private Difficulty difficulty;
    private int currentTurn;

    public AttackPlanner(Random random) {
        this.random = random;
        memory = new ArrayList<>();
        targetShips = new ArrayList<>();
        currentTurn = 0;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void performTurn(AIPlayer ai, Player target) {
        currentTurn++;
        processMemoryForget();
        target.getBoard().clearRecentAttacks();

        int attacks = 0;

        while (attacks < 3) {
            int row;
            int col;
            int[] focusedAttack = null;

            if (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD) {
                focusedAttack = getFocusedAttack(target);
            }

            if (focusedAttack != null) {
                row = focusedAttack[0];
                col = focusedAttack[1];
            } else {
                if (difficulty == Difficulty.EXTREME) {
                    int[] move = getBestProbabilityAttack(ai, target);
                    row = move[0];
                    col = move[1];
                } else {
                    if (difficulty != Difficulty.EASY) {
                        focusedAttack = getFocusedAttack(target);
                    }

                    if (focusedAttack != null) {
                        row = focusedAttack[0];
                        col = focusedAttack[1];
                    } else {
                        row = random.nextInt(ai.getRows());
                        col = random.nextInt(ai.getCols());

                        if (alreadyRemembered(row, col)) {
                            continue;
                        }
                    }
                }
            }

            Tile tile = target.getBoard().getTile(row, col);
            target.getBoard().attackTile(row, col);
            tile.setRecentlyAttacked(true);

            boolean hit = AIPlayer.isHit(tile);

            if (hit) {
                AIPlayer.discoverShip(targetShips, difficulty, tile.getShip(), target.getBoard());
            }

            memory.add(new AttackMemory(row, col, hit, currentTurn));
            attacks++;
        }
    }

    private void processMemoryForget() {
        Iterator<AttackMemory> iterator = memory.iterator();

        while (iterator.hasNext()) {
            AttackMemory mem = iterator.next();
            int age = currentTurn - mem.turnNumber;
            int forgetChance = 0;

            switch (difficulty) {
                case EASY -> {
                    if (age >= 2 && age <= 3) {
                        forgetChance = 30;
                    } else if (age >= 4 && age <= 5) {
                        forgetChance = 35;
                    } else if (age >= 6) {
                        forgetChance = 40;
                    }
                }
                case MEDIUM -> {
                    if (age >= 3 && age <= 4) {
                        forgetChance = 20;
                    } else if (age >= 5 && age <= 6) {
                        forgetChance = 25;
                    } else if (age >= 7) {
                        forgetChance = 30;
                    }
                }
                case HARD -> {
                    if (mem.hit) {
                        continue;
                    }

                    if (age >= 3 && age <= 4) {
                        forgetChance = 10;
                    } else if (age >= 5 && age <= 6) {
                        forgetChance = 15;
                    } else if (age >= 7) {
                        forgetChance = 20;
                    }
                }
                case EXTREME -> {
                    continue;
                }
            }

            int roll = random.nextInt(100);

            if (roll < forgetChance) {
                iterator.remove();
            }
        }
    }

    private boolean alreadyRemembered(int row, int col) {
        for (AttackMemory mem : memory) {
            if (mem.row == row && mem.col == col) {
                return true;
            }
        }

        return false;
    }

    private int[] getFocusedAttack(Player target) {
        Board board = target.getBoard();
        Iterator<Ship> iterator = targetShips.iterator();

        while (iterator.hasNext()) {
            Ship ship = iterator.next();

            if (board.isShipSunk(ship)) {
                iterator.remove();
                continue;
            }

            for (int r = 0; r < board.getRows(); r++) {
                for (int c = 0; c < board.getCols(); c++) {
                    Tile tile = board.getTile(r, c);

                    if (tile.getShip() == ship && tile.isAttacked()) {
                        int[][] dirs = {
                                {-1, 0},
                                {1, 0},
                                {0, -1},
                                {0, 1}
                        };

                        for (int[] d : dirs) {
                            int nr = r + d[0];
                            int nc = c + d[1];

                            if (nr < 0 || nc < 0 || nr >= board.getRows() || nc >= board.getCols()) {
                                continue;
                            }

                            if (alreadyRemembered(nr, nc)) {
                                continue;
                            }

                            return new int[]{nr, nc};
                        }
                    }
                }
            }
        }

        return null;
    }

    private int[][] generateHeatmap(Player target) {
        Board board = target.getBoard();
        int[][] heatmap = new int[board.getRows()][board.getCols()];
        List<HitCluster> clusters = getHitClusters(target);

        for (Ship ship : AIPlayer.possibleShipTypes()) {
            for (Direction dir : Direction.values()) {
                AIPlayer.orientShip(ship, dir);

                for (int row = 0; row < board.getRows(); row++) {
                    for (int col = 0; col < board.getCols(); col++) {
                        boolean valid = true;

                        for (int r = 0; r < ship.getActualHeight(); r++) {
                            for (int c = 0; c < ship.getActualWidth(); c++) {
                                int nr = row + r;
                                int nc = col + c;

                                if (nr >= board.getRows() || nc >= board.getCols()) {
                                    valid = false;
                                    break;
                                }

                                Tile tile = board.getTile(nr, nc);

                                if (tile.isAttacked() && !tile.hasShip()) {
                                    valid = false;
                                    break;
                                }
                            }

                            if (!valid) {
                                break;
                            }
                        }

                        int clusterMatches = countClusterMatches(clusters, ship, row, col);

                        if (valid) {
                            for (int r = 0; r < ship.getActualHeight(); r++) {
                                for (int c = 0; c < ship.getActualWidth(); c++) {
                                    int nr = row + r;
                                    int nc = col + c;

                                    if (!board.getTile(nr, nc).isAttacked()) {
                                        heatmap[nr][nc] += 1 + (clusterMatches * 5);
                                    }
                                }
                            }
                        }
                    }
                }

                if (dir == Direction.VERTICAL) {
                    ship.rotate();
                }
            }
        }

        return heatmap;
    }

    private int countClusterMatches(List<HitCluster> clusters, Ship ship, int row, int col) {
        int clusterMatches = 0;

        for (HitCluster cluster : clusters) {
            boolean clusterCovered = false;

            for (int[] hit : cluster.getHits()) {
                int hr = hit[0];
                int hc = hit[1];

                for (int r = 0; r < ship.getActualHeight(); r++) {
                    for (int c = 0; c < ship.getActualWidth(); c++) {
                        int nr = row + r;
                        int nc = col + c;

                        if (nr == hr && nc == hc) {
                            clusterCovered = true;
                            break;
                        }
                    }

                    if (clusterCovered) {
                        break;
                    }
                }

                if (clusterCovered) {
                    break;
                }
            }

            if (clusterCovered) {
                clusterMatches++;
            }
        }

        return clusterMatches;
    }

    private int[] getBestProbabilityAttack(AIPlayer ai, Player target) {
        int[][] heatmap = generateHeatmap(target);
        int bestScore = -1;
        List<int[]> bestTiles = new ArrayList<>();

        for (int r = 0; r < ai.getRows(); r++) {
            for (int c = 0; c < ai.getCols(); c++) {
                if (alreadyRemembered(r, c)) {
                    continue;
                }

                int score = heatmap[r][c];

                if (score > bestScore) {
                    bestScore = score;
                    bestTiles.clear();
                    bestTiles.add(new int[]{r, c});
                } else if (score == bestScore) {
                    bestTiles.add(new int[]{r, c});
                }
            }
        }

        return bestTiles.get(random.nextInt(bestTiles.size()));
    }

    private void floodFillCluster(
            Board board,
            boolean[][] visited,
            HitCluster cluster,
            int row,
            int col
    ) {
        if (row < 0 || row >= board.getRows() || col < 0 || col >= board.getCols()) {
            return;
        }

        if (visited[row][col]) {
            return;
        }

        Tile tile = board.getTile(row, col);

        if (!(tile.isAttacked() && tile.hasShip())) {
            return;
        }

        visited[row][col] = true;
        cluster.add(row, col);

        int[][] dirs = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1}
        };

        for (int[] d : dirs) {
            floodFillCluster(board, visited, cluster, row + d[0], col + d[1]);
        }
    }

    private List<HitCluster> getHitClusters(Player target) {
        Board board = target.getBoard();
        List<HitCluster> clusters = new ArrayList<>();
        boolean[][] visited = new boolean[board.getRows()][board.getCols()];

        for (int r = 0; r < board.getRows(); r++) {
            for (int c = 0; c < board.getCols(); c++) {
                Tile tile = board.getTile(r, c);

                if (visited[r][c]) {
                    continue;
                }

                if (!(tile.isAttacked() && tile.hasShip())) {
                    continue;
                }

                HitCluster cluster = new HitCluster();
                floodFillCluster(board, visited, cluster, r, c);
                clusters.add(cluster);
            }
        }

        return clusters;
    }
}
