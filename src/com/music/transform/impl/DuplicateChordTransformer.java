package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Duplicate Chords – repeat each chord’s intervals.
 * [2,5,9] → [2,5,9,2,5,9]
 */
public class DuplicateChordTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            int[] row = orig[i];
            out[i] = new int[row.length * 2];
            for (int j = 0; j < row.length; j++) {
                out[i][j] = row[j];
                out[i][j + row.length] = row[j];
            }
        }
        return new Cadence(
            "Duplicate Chords of " + c.type(),
            out,
            null,
            "Duplicate Chords – repeat each chord"
        );
    }
}
