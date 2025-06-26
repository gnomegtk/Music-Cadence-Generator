package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Diminution – halve all intervals (integer division).
 * E.g. [2,5,9] → [1,2,4]
 */
public class DiminutionTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                out[i][j] = orig[i][j] / 2;
            }
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, out);
        return new Cadence(
            "Diminution of " + c.type(),
            out,
            notes,
            "Diminution – ÷2 all intervals, e.g. [2,5,9] → [1,2,4]"
        );
    }
}
