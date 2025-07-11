package com.music.registry;

import com.music.domain.Cadence;
import java.util.*;

/**
 * Defines raw C-major cadences as semitone-interval matrices.
 * Returns pure-interval Cadence objects (matrix=null).
 */
public class CadenceRegistry {

    private static final Map<String,int[][]> CADENCES = new LinkedHashMap<>();

    static {
        CADENCES.put("ii–V–I (Maj)",  new int[][]{{2,5,9},{7,11,14},{0,4,7}});
        CADENCES.put("II–V–I (Maj)",  new int[][]{{2,6,9},{7,11,14},{0,4,7}});
        CADENCES.put("IV–V–I (Maj)",  new int[][]{{5,9,12},{7,11,14},{0,4,7}});
        CADENCES.put("ii–V–I (Min)",  new int[][]{{2,5,9},{7,11,14},{0,3,7}});
        CADENCES.put("II–V–I (Min)",  new int[][]{{2,6,9},{7,11,14},{0,3,7}});
        CADENCES.put("IV–V–I (Min)",  new int[][]{{5,8,12},{7,11,14},{0,3,7}});
        CADENCES.put("Deceptive",     new int[][]{{7,11,14},{9,12,16},{0,4,7}});
        CADENCES.put("ii°–V–i",       new int[][]{{2,5,8},{7,11,14},{0,3,7}});
        CADENCES.put("I–vi–ii–V",     new int[][]{{0,4,7},{9,12,16},{2,5,9},{7,11,14}});
        CADENCES.put("Blues I–IV–V",  new int[][]{{0,4,7},{5,9,12},{7,10,14}});
        CADENCES.put("iii–vi–ii–V–I", new int[][]{
            {4,7,11},{9,12,16},{2,5,9},{7,11,14},{0,4,7}
        });
    }

    public static List<String> getAvailableCadences() {
        return new ArrayList<>(CADENCES.keySet());
    }

    /**
     * Returns a Cadence carrying raw intervals only.
     */
    public static Cadence getCadence(String name) {
        int[][] semis = CADENCES.getOrDefault(name, new int[0][]);
        return new Cadence(name, semis, null, name + " (raw intervals)");
    }
}
