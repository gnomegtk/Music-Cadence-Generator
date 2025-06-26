package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Augmentation – double all intervals.
 * E.g. [2,5,9] → [4,10,18]
 */
public class AugmentationTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                out[i][j] = orig[i][j] * 2;
            }
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, out);
        return new Cadence(
            "Augmentation of " + c.type(),
            out,
            notes,
            "Augmentation – ×2 all intervals, e.g. [2,5,9] → [4,10,18]"
        );
    }
}
