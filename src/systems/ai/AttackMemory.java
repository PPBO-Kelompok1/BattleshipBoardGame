package systems.ai;

class AttackMemory {

    final int row;
    final int col;
    final boolean hit;
    final int turnNumber;

    AttackMemory(int row, int col, boolean hit, int turnNumber) {
        this.row = row;
        this.col = col;
        this.hit = hit;
        this.turnNumber = turnNumber;
    }
}
