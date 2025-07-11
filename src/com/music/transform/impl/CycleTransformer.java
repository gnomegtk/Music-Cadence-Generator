package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Cycle – rotate chord sequence forward one.
 * [[2,5,9],[7,11,14],[0,4,7]] →
 * [[7,11,14],[0,4,7],[2,5,9]]
 */
public class CycleTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int n = orig.length;
        int[][] out = new int[n][];
        for (int i = 0; i < n; i++) {
            out[i] = orig[(i + 1) % n];
        }
        return new Cadence(
            "Cycle of " + c.type(),
            out,
            null,
            "Cycle – rotate forward one step"
        );
    }
}
