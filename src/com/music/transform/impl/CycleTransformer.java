package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Cycle – rotate chords forward one step.
 * E.g. [[2,5,9],[7,11,14],[0,4,7]] →
 *      [[7,11,14],[0,4,7],[2,5,9]]
 */
public class CycleTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int n = orig.length;
        int[][] cyc = new int[n][];
        for (int i = 0; i < n; i++) {
            cyc[i] = orig[(i + 1) % n];
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, cyc);
        return new Cadence(
            "Cycle of " + c.type(),
            cyc,
            notes,
            "Cycle – rotate forward, e.g. [[2,5,9],[7,11,14],[0,4,7]] → [[7,11,14],[0,4,7],[2,5,9]]"
        );
    }
}
