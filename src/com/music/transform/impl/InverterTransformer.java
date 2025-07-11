package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Inversion – mirror each chord’s intervals.
 * [2,5,9] → [9,5,2]
 */
public class InverterTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            int[] row = orig[i];
            out[i] = new int[row.length];
            for (int j = 0; j < row.length; j++) {
                out[i][j] = row[row.length - 1 - j];
            }
        }
        return new Cadence(
            "Inversion of " + c.type(),
            out,
            null,
            "Inversion – reverse each chord’s intervals"
        );
    }
}
