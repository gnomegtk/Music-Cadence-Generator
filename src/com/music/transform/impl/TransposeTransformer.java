package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Transpose +2 – shift all intervals up by 2 semitones.
 * [2,5,9] → [4,7,11]
 */
public class TransposeTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                out[i][j] = orig[i][j] + 2;
            }
        }
        return new Cadence(
            "Transposed +2 of " + c.type(),
            out,
            null,
            "Transpose +2 – shift up by 2 semitones"
        );
    }
}
