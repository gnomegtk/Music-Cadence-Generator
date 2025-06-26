package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Reciprocal – replace each interval x with (12 – x).
 * E.g. [2,5,9] → [10,7,3]
 */
public class ReciprocalTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                out[i][j] = 12 - (orig[i][j] % 12);
            }
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, out);
        return new Cadence(
            "Reciprocal of " + c.type(),
            out,
            notes,
            "Reciprocal – (12 – x), e.g. [2,5,9] → [10,7,3]"
        );
    }
}
