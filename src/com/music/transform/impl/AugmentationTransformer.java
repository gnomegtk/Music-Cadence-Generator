package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Augmentation – double all intervals.
 * [2,5,9] → [4,10,18]
 */
public class AugmentationTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                out[i][j] = orig[i][j] * 2;
            }
        }
        return new Cadence(
            "Augmentation of " + c.type(),
            out,
            null,
            "Augmentation – ×2 all intervals"
        );
    }
}
