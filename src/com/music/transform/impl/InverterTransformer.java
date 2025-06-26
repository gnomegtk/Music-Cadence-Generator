package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Inversion – mirror each chord’s intervals.
 * E.g. [2,5,9] → [9,5,2]
 */
public class InverterTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] inv  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            inv[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                inv[i][j] = orig[i][orig[i].length - 1 - j];
            }
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, inv);
        return new Cadence(
            "Inversion of " + c.type(),
            inv,
            notes,
            "Inversion – each chord [a,b,c] → [c,b,a], e.g. [2,5,9] → [9,5,2]"
        );
    }
}
