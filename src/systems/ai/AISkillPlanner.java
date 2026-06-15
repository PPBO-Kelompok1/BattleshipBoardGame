package systems.ai;

import config.GameConfig;
import core.Difficulty;
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
import physics.Board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AISkillPlanner {

    private final Random random;
    private final List<int[]> hintedTiles;
    private String lastSkillMessage;

    public AISkillPlanner(Random random) {
        this.random = random;
        hintedTiles = new ArrayList<>();
        lastSkillMessage = "";
    }

    public int tryUseSkill(AIPlayer ai, Player target, AttackPlanner attackPlanner) {
        lastSkillMessage = "";
        Difficulty difficulty = attackPlanner.getDifficulty();

        if (difficulty == Difficulty.EASY) {
            return 0;
        }

        List<Ship> readyShips = new ArrayList<>();

        for (Ship ship : ai.getShips()) {
            if (ship.canUseSkill()) {
                readyShips.add(ship);
            }
        }

        if (readyShips.isEmpty()) {
            return 0;
        }

        if (difficulty == Difficulty.MEDIUM && random.nextInt(100) >= 30) {
            return 0;
        }

        if (difficulty == Difficulty.HARD && random.nextInt(100) >= 60) {
            return 0;
        }

        if (difficulty == Difficulty.EXTREME) {
            readyShips.sort((first, second) -> Integer.compare(
                    estimateUsefulness(second, ai, target, attackPlanner),
                    estimateUsefulness(first, ai, target, attackPlanner)
            ));
        } else {
            Collections.shuffle(readyShips, random);
        }

        for (Ship ship : readyShips) {
            int attacksConsumed = useShipSkill(ship, ai, target, attackPlanner);

            if (ship.isSkillUsed()) {
                return attacksConsumed;
            }
        }

        return 0;
    }

    public String getLastSkillMessage() {
        return lastSkillMessage;
    }

    public int[] pollHintedTarget(Player target) {
        Board board = target.getBoard();

        for (int i = 0; i < hintedTiles.size(); i++) {
            int[] hint = hintedTiles.get(i);

            if (!board.isInside(hint[0], hint[1]) || board.getTile(hint[0], hint[1]).isAttacked()) {
                hintedTiles.remove(i);
                i--;
                continue;
            }

            hintedTiles.remove(i);
            return hint;
        }

        return null;
    }

    public void removeHint(int row, int col) {
        hintedTiles.removeIf(hint -> hint[0] == row && hint[1] == col);
    }

    private int useShipSkill(Ship ship, AIPlayer ai, Player target, AttackPlanner attackPlanner) {
        if (ship instanceof Destroyer) {
            return useDestroyer((Destroyer) ship, target, attackPlanner);
        }

        if (ship instanceof Battleship) {
            return useBattleship((Battleship) ship, target, attackPlanner);
        }

        if (ship instanceof Submarine) {
            return useSubmarine((Submarine) ship, target, attackPlanner);
        }

        if (ship instanceof PhantomCruiser) {
            return usePhantomCruiser((PhantomCruiser) ship, ai);
        }

        if (ship instanceof RadarCruiser) {
            return useRadarCruiser((RadarCruiser) ship, target, attackPlanner);
        }

        if (ship instanceof Carrier) {
            return useCarrier((Carrier) ship, ai);
        }

        return 0;
    }

    private int useDestroyer(Destroyer destroyer, Player target, AttackPlanner attackPlanner) {
        List<int[]> targets = new ArrayList<>(attackPlanner.getFocusedTargets(target, 2));

        if (targets.size() < 2) {
            for (int[] tile : attackPlanner.getBestUnattackedTiles(target, 2)) {
                addUniqueTarget(targets, tile[0], tile[1]);

                if (targets.size() == 2) {
                    break;
                }
            }
        }

        int attacks = 0;

        for (int[] tile : targets) {
            if (attacks >= 2) {
                break;
            }

            if (attackPlanner.performAttack(target, tile[0], tile[1])) {
                attacks++;
            }
        }

        if (attacks > 0) {
            destroyer.markSkillUsed();
            lastSkillMessage = "AI used Double Strike.";
        }

        return attacks;
    }

    private int useBattleship(Battleship battleship, Player target, AttackPlanner attackPlanner) {
        Board board = target.getBoard();
        int[][] heatmap = attackPlanner.buildHeatmap(target);
        int threshold = bombardmentThreshold(attackPlanner.getDifficulty());
        int bestScore = -1;
        List<int[]> bestAreas = new ArrayList<>();

        for (int row = 0; row <= board.getRows() - 2; row++) {
            for (int col = 0; col <= board.getCols() - 2; col++) {
                int score = scoreArea(board, heatmap, row, col, 2, 2);

                if (score > bestScore) {
                    bestScore = score;
                    bestAreas.clear();
                }

                if (score == bestScore) {
                    bestAreas.add(new int[]{row, col});
                }
            }
        }

        if (bestScore < threshold || bestAreas.isEmpty()) {
            return 0;
        }

        int[] area = bestAreas.get(random.nextInt(bestAreas.size()));
        int attacks = 0;

        for (int row = area[0]; row < area[0] + 2; row++) {
            for (int col = area[1]; col < area[1] + 2; col++) {
                if (attacks >= GameConfig.ATTACKS_PER_TURN) {
                    break;
                }

                if (attackPlanner.performAttack(target, row, col)) {
                    attacks++;
                }
            }
        }

        if (attacks > 0) {
            battleship.markSkillUsed();
            lastSkillMessage = "AI used Area Bombardment.";
        }

        return attacks;
    }

    private int useSubmarine(Submarine submarine, Player target, AttackPlanner attackPlanner) {
        Board board = target.getBoard();
        int[][] heatmap = attackPlanner.buildHeatmap(target);
        int[] area = findBestArea(board, heatmap, 2, 2);

        if (area == null) {
            return 0;
        }

        int detectedSegments = scanArea(board, area[0], area[1], 2, 2, true);
        submarine.markSkillUsed();
        lastSkillMessage = detectedSegments > 0
                ? "AI used Sonar Scan. Detected ship presence in a 2x2 sector."
                : "AI used Sonar Scan. No ship presence detected.";
        return 0;
    }

    private int usePhantomCruiser(PhantomCruiser phantomCruiser, AIPlayer ai) {
        Board board = ai.getBoard();
        int bestScore = Integer.MIN_VALUE;
        List<int[]> bestPositions = new ArrayList<>();

        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                if (!board.canRelocateShip(phantomCruiser, row, col)) {
                    continue;
                }

                int score = scoreRelocation(board, phantomCruiser, row, col);

                if (score > bestScore) {
                    bestScore = score;
                    bestPositions.clear();
                }

                if (score == bestScore) {
                    bestPositions.add(new int[]{row, col});
                }
            }
        }

        if (bestPositions.isEmpty()) {
            return 0;
        }

        int[] position = bestPositions.get(random.nextInt(bestPositions.size()));

        if (!board.relocateShip(phantomCruiser, position[0], position[1])) {
            return 0;
        }

        phantomCruiser.markSkillUsed();
        lastSkillMessage = "AI used Shadow Relocation.";
        return 0;
    }

    private int useRadarCruiser(RadarCruiser radarCruiser, Player target, AttackPlanner attackPlanner) {
        Board board = target.getBoard();
        int[][] heatmap = attackPlanner.buildHeatmap(target);
        int[] center = findBestCenterArea(board, heatmap, 3, 3);

        if (center == null) {
            return 0;
        }

        int detectedSegments = scanArea(board, center[0] - 1, center[1] - 1, 3, 3, true);
        radarCruiser.markSkillUsed();
        lastSkillMessage = detectedSegments > 0
                ? "AI used Radar Sweep. Detected ship presence in a 3x3 sector."
                : "AI used Radar Sweep. No ship presence detected.";
        return 0;
    }

    private int useCarrier(Carrier carrier, AIPlayer ai) {
        Board board = ai.getBoard();
        List<int[]> candidates = getDecoyCandidates(board);

        if (candidates.size() < 2) {
            return 0;
        }

        Collections.shuffle(candidates, random);
        int deployed = 0;
        List<DecoyShip> placedDecoys = new ArrayList<>();

        for (int[] candidate : candidates) {
            if (deployed >= 2) {
                break;
            }

            DecoyShip decoy = new DecoyShip();

            if (board.placeDecoyShip(decoy, candidate[0], candidate[1])) {
                deployed++;
                placedDecoys.add(decoy);
            }
        }

        if (deployed < 2) {
            for (DecoyShip decoy : placedDecoys) {
                board.clearShipPlacement(decoy);
            }

            return 0;
        }

        carrier.markSkillUsed();
        lastSkillMessage = "AI used Decoy Deployment and deployed 2 decoy ships.";
        return 0;
    }

    private int estimateUsefulness(Ship ship, AIPlayer ai, Player target, AttackPlanner attackPlanner) {
        if (ship instanceof PhantomCruiser) {
            return ship.getDamageTaken() * 10 + countRelocationOptions(ai.getBoard(), ship);
        }

        Board board = target.getBoard();
        int[][] heatmap = attackPlanner.buildHeatmap(target);

        if (ship instanceof Destroyer) {
            return sumTargets(attackPlanner.getFocusedTargets(target, 2), heatmap)
                    + sumTargets(attackPlanner.getBestUnattackedTiles(target, 2), heatmap);
        }

        if (ship instanceof Battleship) {
            return bestAreaScore(board, heatmap, 2, 2);
        }

        if (ship instanceof Submarine) {
            return bestAreaScore(board, heatmap, 2, 2) - 2;
        }

        if (ship instanceof RadarCruiser) {
            return bestAreaScore(board, heatmap, 3, 3) - 1;
        }

        if (ship instanceof Carrier) {
            return ship.getDamageTaken() * 10 + countValidDecoyPositions(ai.getBoard());
        }

        return 0;
    }

    private List<int[]> getDecoyCandidates(Board board) {
        List<int[]> candidates = new ArrayList<>();

        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                if (board.canPlaceDecoyShip(new DecoyShip(), row, col)) {
                    candidates.add(new int[]{row, col});
                }
            }
        }

        return candidates;
    }

    private int countValidDecoyPositions(Board board) {
        return getDecoyCandidates(board).size();
    }

    private int bombardmentThreshold(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> 10;
            case HARD -> 8;
            case EXTREME -> 6;
            default -> Integer.MAX_VALUE;
        };
    }

    private int scanArea(Board board, int startRow, int startCol, int height, int width, boolean addHints) {
        int detectedSegments = 0;
        List<int[]> scannedTiles = new ArrayList<>();

        for (int row = startRow; row < startRow + height; row++) {
            for (int col = startCol; col < startCol + width; col++) {
                if (!board.isInside(row, col)) {
                    continue;
                }

                board.markInteracted(row, col);
                scannedTiles.add(new int[]{row, col});

                if (board.getTile(row, col).hasShip()) {
                    detectedSegments++;
                }
            }
        }

        if (addHints && detectedSegments > 0) {
            for (int[] tile : scannedTiles) {
                if (!board.getTile(tile[0], tile[1]).isAttacked()) {
                    addUniqueTarget(hintedTiles, tile[0], tile[1]);
                }
            }
        }

        return detectedSegments;
    }

    private int[] findBestArea(Board board, int[][] heatmap, int height, int width) {
        int bestScore = -1;
        List<int[]> bestAreas = new ArrayList<>();

        for (int row = 0; row <= board.getRows() - height; row++) {
            for (int col = 0; col <= board.getCols() - width; col++) {
                int score = scoreArea(board, heatmap, row, col, height, width);

                if (score > bestScore) {
                    bestScore = score;
                    bestAreas.clear();
                }

                if (score == bestScore) {
                    bestAreas.add(new int[]{row, col});
                }
            }
        }

        if (bestAreas.isEmpty()) {
            return null;
        }

        return bestAreas.get(random.nextInt(bestAreas.size()));
    }

    private int[] findBestCenterArea(Board board, int[][] heatmap, int height, int width) {
        int[] topLeft = findBestArea(board, heatmap, height, width);

        if (topLeft == null) {
            return null;
        }

        return new int[]{topLeft[0] + height / 2, topLeft[1] + width / 2};
    }

    private int scoreArea(Board board, int[][] heatmap, int startRow, int startCol, int height, int width) {
        int score = 0;

        for (int row = startRow; row < startRow + height; row++) {
            for (int col = startCol; col < startCol + width; col++) {
                if (!board.isInside(row, col)) {
                    return -1;
                }

                if (!board.getTile(row, col).isAttacked()) {
                    score += heatmap[row][col];
                }
            }
        }

        return score;
    }

    private int bestAreaScore(Board board, int[][] heatmap, int height, int width) {
        int bestScore = -1;

        for (int row = 0; row <= board.getRows() - height; row++) {
            for (int col = 0; col <= board.getCols() - width; col++) {
                bestScore = Math.max(bestScore, scoreArea(board, heatmap, row, col, height, width));
            }
        }

        return bestScore;
    }

    private int scoreRelocation(Board board, Ship ship, int row, int col) {
        int minDistance = board.getRows() + board.getCols();

        for (int boardRow = 0; boardRow < board.getRows(); boardRow++) {
            for (int boardCol = 0; boardCol < board.getCols(); boardCol++) {
                if (!board.wasInteracted(boardRow, boardCol)) {
                    continue;
                }

                for (int shipRow = row; shipRow < row + ship.getActualHeight(); shipRow++) {
                    for (int shipCol = col; shipCol < col + ship.getActualWidth(); shipCol++) {
                        int distance = Math.abs(shipRow - boardRow) + Math.abs(shipCol - boardCol);
                        minDistance = Math.min(minDistance, distance);
                    }
                }
            }
        }

        return minDistance;
    }

    private int countRelocationOptions(Board board, Ship ship) {
        int count = 0;

        for (int row = 0; row < board.getRows(); row++) {
            for (int col = 0; col < board.getCols(); col++) {
                if (board.canRelocateShip(ship, row, col)) {
                    count++;
                }
            }
        }

        return count;
    }

    private int sumTargets(List<int[]> targets, int[][] heatmap) {
        int sum = 0;

        for (int[] target : targets) {
            sum += heatmap[target[0]][target[1]];
        }

        return sum;
    }

    private void addUniqueTarget(List<int[]> targets, int row, int col) {
        for (int[] target : targets) {
            if (target[0] == row && target[1] == col) {
                return;
            }
        }

        targets.add(new int[]{row, col});
    }
}
