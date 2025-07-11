package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;
import java.util.Map;
import java.util.HashMap;

/**
 * Adds the tonic’s semitone index (C→0, C#→1, … B→11)
 * to raw C-based offsets. Outputs pure semitone grids.
 */
public class TransposeToTonicTransformer implements Transformer {

    private static final Map<String,Integer> TONIC_SHIFTS = new HashMap<>();
    static {
        TONIC_SHIFTS.put("C",  0);
        TONIC_SHIFTS.put("C#", 1); TONIC_SHIFTS.put("Db", 1);
        TONIC_SHIFTS.put("D",  2);
        TONIC_SHIFTS.put("D#", 3); TONIC_SHIFTS.put("Eb", 3);
        TONIC_SHIFTS.put("E",  4);
        TONIC_SHIFTS.put("F",  5);
        TONIC_SHIFTS.put("F#", 6); TONIC_SHIFTS.put("Gb", 6);
        TONIC_SHIFTS.put("G",  7);
        TONIC_SHIFTS.put("G#", 8); TONIC_SHIFTS.put("Ab", 8);
        TONIC_SHIFTS.put("A",  9);
        TONIC_SHIFTS.put("A#",10); TONIC_SHIFTS.put("Bb",10);
        TONIC_SHIFTS.put("B", 11); TONIC_SHIFTS.put("Cb",11);
    }

    private final int shift;
    private final String tonic;

    public TransposeToTonicTransformer(String tonic) {
        this.tonic = tonic;
        this.shift = TONIC_SHIFTS.getOrDefault(tonic, 0);
    }

    public Cadence transform(Cadence raw) {
        System.out.printf("=== DEBUG Transpose: tonic=%s shift=%d%n", tonic, shift);
        int[][] src = raw.intervals();
        int[][] dst = new int[src.length][];

        for (int i = 0; i < src.length; i++) {
            dst[i] = new int[src[i].length];
            System.out.printf("DEBUG Transpose: chord %d raw=%s%n",
                              i+1, java.util.Arrays.toString(src[i]));
            for (int j = 0; j < src[i].length; j++) {
                dst[i][j] = src[i][j] + shift;
            }
            System.out.printf("DEBUG Transpose: chord %d semitones=%s%n",
                              i+1, java.util.Arrays.toString(dst[i]));
        }

        // Cadence(type, semitoneGrid, spelledNotes=null, description)
        return new Cadence(raw.type(), dst, null, raw.description());
    }
}
