package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Reciprocal – replace x with (12 – (x mod 12)).
 * [2,5,9] → [10,7,3]
 */
public class ReciprocalTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                out[i][j] = 12 - (orig[i][j] % 12);
            }
        }
        return new Cadence(
            "Reciprocal of " + c.type(),
            out,
            null,
            "Reciprocal – (12 – x)"
        );
    }
}
