package com.music.registry;

import com.music.domain.Cadence;
import com.music.domain.Note;

import java.util.*;

/**
 * Defines 3-step cadences as semitone-interval matrices.
 * Transposes each into Note[][] by a given tonic.
 */
public class CadenceRegistry {
    private static final String[] CHROMA = {
        "C","C#","D","D#","E","F","F#","G","G#","A","A#","B"
    };

    private static final Map<String,int[][]> CADENCES = new LinkedHashMap<>();
    static {
        CADENCES.put("II–V–I (Maj)",  new int[][]{{2,6,9},{7,11,14},{0,4,7}});
        CADENCES.put("IV–V–I (Maj)",  new int[][]{{5,9,12},{7,11,14},{0,4,7}});
        CADENCES.put("II–V–I (Min)",  new int[][]{{2,5,9},{7,11,14},{0,3,7}});
        CADENCES.put("IV–V–I (Min)",  new int[][]{{5,8,12},{7,11,14},{0,3,7}});
        CADENCES.put("Deceptive",     new int[][]{{7,11,14},{9,12,16},{0,4,7}});
    	CADENCES.put("II°–V–i",       new int[][]{{2,5,8},{7,11,14},{0,3,7}});
    	CADENCES.put("I–vi–ii–V",     new int[][]{{0,4,7},{9,12,16},{2,5,9},{7,11,14}});
    	CADENCES.put("Blues I–IV–V",  new int[][]{{0,4,7},{5,9,12},{7,10,14}});
    	CADENCES.put("iii–vi–ii–V–I", new int[][]{
    	    {4,7,11},{9,12,16},{2,5,9},{7,11,14},{0,4,7}
    	});
    }

    public static List<String> getAvailableCadences() {
        return new ArrayList<>(CADENCES.keySet());
    }

    public static Cadence getCadence(String name, String tonic) {
        int[][] semis = CADENCES.getOrDefault(name, new int[0][]);
        Note[][] notes = transposeMatrix(tonic, semis);
        return new Cadence(name, semis, notes, name + " in " + tonic);
    }

    public static Note[][] transposeMatrix(String tonic, int[][] semis) {
        int root = Arrays.asList(CHROMA).indexOf(tonic);
        Note[][] out = new Note[semis.length][];
        for (int i = 0; i < semis.length; i++) {
            out[i] = new Note[semis[i].length];
            for (int j = 0; j < semis[i].length; j++) {
                int idx = (root + (semis[i][j] % 12) + 12) % 12;
                out[i][j] = new Note(CHROMA[idx]);
            }
        }
        return out;
    }
}
