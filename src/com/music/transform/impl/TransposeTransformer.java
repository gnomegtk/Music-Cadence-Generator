package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Transpose +2 – shift every interval up by 2 semitones.
 * E.g. [2,5,9] → [4,7,11]
 */
public class TransposeTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                out[i][j] = orig[i][j] + 2;
            }
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, out);
        return new Cadence(
            "Transposed +2 of " + c.type(),
            out,
            notes,
            "Transpose +2 – e.g. [2,5,9] → [4,7,11]"
        );
    }
}
