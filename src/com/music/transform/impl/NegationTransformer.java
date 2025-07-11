package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Negation – reflect intervals around zero.
 * [2,5,9] → [-2,-5,-9]
 */
public class NegationTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                out[i][j] = -orig[i][j];
            }
        }
        return new Cadence(
            "Negation of " + c.type(),
            out,
            null,
            "Negation – x → -x"
        );
    }
}
