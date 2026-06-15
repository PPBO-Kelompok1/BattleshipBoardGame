package systems.ai;

import config.GameConfig;
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
    private final AISkillPlanner aiSkillPlanner;
    private Difficulty difficulty;
    private int currentTurn;
    private String lastAiSkillMessage;
    private int lastDestroyedDecoyCount;

    public AttackPlanner(Random random) {
        this.random = random;
        memory = new ArrayList<>();
        targetShips = new ArrayList<>();
        aiSkillPlanner = new AISkillPlanner(random);
        currentTurn = 0;
        lastAiSkillMessage = "";
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getLastAiSkillMessage() {
        return lastAiSkillMessage;
    }

    public int getLastDestroyedDecoyCount() {
        return lastDestroyedDecoyCount;
    }

    public void performTurn(AIPlayer ai, Player target) {
        currentTurn++;
        processMemoryForget();
        target.getBoard().clearRecentAttacks();
        lastAiSkillMessage = "";
        lastDestroyedDecoyCount = 0;

        int attacks = aiSkillPlanner.tryUseSkill(ai, target, this);
        lastAiSkillMessage = aiSkillPlanner.getLastSkillMessage();

        while (attacks < GameConfig.ATTACKS_PER_TURN) {
            int row;
            int col;
            int[] selected = selectNormalAttack(ai, target);

            if (selected == null) {
                break;
            }

            row = selected[0];
            col = selected[1];

            if (!performAttack(target, row, col)) {
                continue;
            }

            attacks++;
        }
    }

    private int[] selectNormalAttack(AIPlayer ai, Player target) {
        int[] focusedAttack = null;

        if (difficulty == Difficulty.MEDIUM || difficulty == Difficulty.HARD || difficulty == Difficulty.EXTREME) {
            focusedAttack = getFocusedAttack(target);
        }

        if (focusedAttack != null) {
            return focusedAttack;
        }

        int[] hintedAttack = aiSkillPlanner.pollHintedTarget(target);

        if (hintedAttack != null) {
            return hintedAttack;
        }

        if (difficulty == Difficulty.EXTREME) {
            return getBestProbabilityAttack(ai, target);
        }

        List<int[]> candidates = new ArrayList<>();

        for (int row = 0; row < target.getBoard().getRows(); row++) {
            for (int col = 0; col < target.getBoard().getCols(); col++) {
                if (!target.getBoard().getTile(row, col).isAttacked() && !alreadyRemembered(row, col)) {
                    candidates.add(new int[]{row, col});
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    public boolean performAttack(Player target, int row, int col) {
        Board board = target.getBoard();

        if (!board.isInside(row, col)) {
            return false;
        }

        Tile tile = board.getTile(row, col);

        if (tile.isAttacked()) {
            return false;
        }

        Ship shipBefore = tile.getShip();
        board.attackTile(row, col);
        tile.setRecentlyAttacked(true);

        if (shipBefore != null && shipBefore.isSunk() && !shipBefore.countsForVictory()) {
            lastDestroyedDecoyCount++;
        }

        boolean hit = AIPlayer.isHit(tile);

        if (hit) {
            AIPlayer.discoverShip(targetShips, difficulty, tile.getShip(), board);
        }

        rememberAttack(row, col, hit);
        aiSkillPlanner.removeHint(row, col);
        return true;
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

    public boolean alreadyRemembered(int row, int col) {
        for (AttackMemory mem : memory) {
            if (mem.row == row && mem.col == col) {
                return true;
            }
        }

        return false;
    }

    public void rememberAttack(int row, int col, boolean hit) {
        memory.add(new AttackMemory(row, col, hit, currentTurn));
    }

    private int[] getFocusedAttack(Player target) {
        List<int[]> focusedTargets = getFocusedTargets(target, 1);

        if (focusedTargets.isEmpty()) {
            return null;
        }

        return focusedTargets.get(0);
    }

    public List<int[]> getFocusedTargets(Player target, int count) {
        Board board = target.getBoard();
        Iterator<Ship> iterator = targetShips.iterator();
        List<int[]> targets = new ArrayList<>();

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

                            if (board.getTile(nr, nc).isAttacked()) {
                                continue;
                            }

                            addUniqueTarget(targets, nr, nc);

                            if (targets.size() == count) {
                                return targets;
                            }
                        }
                    }
                }
            }
        }

        return targets;
    }

    public int[][] buildHeatmap(Player target) {
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

    private int[][] generateHeatmap(Player target) {
        return buildHeatmap(target);
    }

    public List<int[]> getBestUnattackedTiles(Player target, int count) {
        Board board = target.getBoard();
        int[][] heatmap = buildHeatmap(target);
        List<int[]> selected = new ArrayList<>();

        while (selected.size() < count) {
            int bestScore = -1;
            List<int[]> bestTiles = new ArrayList<>();

            for (int row = 0; row < board.getRows(); row++) {
                for (int col = 0; col < board.getCols(); col++) {
                    if (board.getTile(row, col).isAttacked() || alreadyRemembered(row, col) || containsTarget(selected, row, col)) {
                        continue;
                    }

                    int score = heatmap[row][col];

                    if (score > bestScore) {
                        bestScore = score;
                        bestTiles.clear();
                    }

                    if (score == bestScore) {
                        bestTiles.add(new int[]{row, col});
                    }
                }
            }

            if (bestTiles.isEmpty()) {
                break;
            }

            selected.add(bestTiles.get(random.nextInt(bestTiles.size())));
        }

        return selected;
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
        int[][] heatmap = buildHeatmap(target);
        int bestScore = -1;
        List<int[]> bestTiles = new ArrayList<>();

        for (int r = 0; r < target.getBoard().getRows(); r++) {
            for (int c = 0; c < target.getBoard().getCols(); c++) {
                if (alreadyRemembered(r, c) || target.getBoard().getTile(r, c).isAttacked()) {
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

        if (bestTiles.isEmpty()) {
            return null;
        }

        return bestTiles.get(random.nextInt(bestTiles.size()));
    }

    private void addUniqueTarget(List<int[]> targets, int row, int col) {
        if (!containsTarget(targets, row, col)) {
            targets.add(new int[]{row, col});
        }
    }

    private boolean containsTarget(List<int[]> targets, int row, int col) {
        for (int[] target : targets) {
            if (target[0] == row && target[1] == col) {
                return true;
            }
        }

        return false;
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
