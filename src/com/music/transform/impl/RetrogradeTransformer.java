package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Retrograde – reverse the chord order.
 * E.g. [[2,5,9],[7,11,14],[0,4,7]] →
 *      [[0,4,7],[7,11,14],[2,5,9]]
 */
public class RetrogradeTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int n = orig.length;
        int[][] rev = new int[n][];
        for (int i = 0; i < n; i++) {
            rev[i] = orig[n - 1 - i];
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, rev);
        return new Cadence(
            "Retrograde of " + c.type(),
            rev,
            notes,
            "Retrograde – progression reversed, e.g. [[2,5,9],[7,11,14],[0,4,7]] → [[0,4,7],[7,11,14],[2,5,9]]"
        );
    }
}
