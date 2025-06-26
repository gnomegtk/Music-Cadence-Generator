package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Negation – reflect intervals around zero.
 * E.g. [2,5,9] → [-2,-5,-9]
 */
public class NegationTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] neg  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            neg[i] = new int[orig[i].length];
            for (int j = 0; j < orig[i].length; j++) {
                neg[i][j] = -orig[i][j];
            }
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, neg);
        return new Cadence(
            "Negation of " + c.type(),
            neg,
            notes,
            "Negation – x → -x, e.g. [2,5,9] → [-2,-5,-9]"
        );
    }
}
