package systems.ai;

import java.util.ArrayList;
import java.util.List;

class HitCluster {

    private final List<int[]> hits;

    HitCluster() {
        hits = new ArrayList<>();
    }

    void add(int row, int col) {
        hits.add(new int[]{row, col});
    }

    List<int[]> getHits() {
        return hits;
    }
}
