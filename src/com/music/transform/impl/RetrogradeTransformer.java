package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Retrograde – reverse chord order.
 * [[2,5,9],[7,11,14],[0,4,7]] →
 * [[0,4,7],[7,11,14],[2,5,9]]
 */
public class RetrogradeTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int n = orig.length;
        int[][] out = new int[n][];
        for (int i = 0; i < n; i++) {
            out[i] = orig[n - 1 - i];
        }
        return new Cadence(
            "Retrograde of " + c.type(),
            out,
            null,
            "Retrograde – reverse progression"
        );
    }
}
