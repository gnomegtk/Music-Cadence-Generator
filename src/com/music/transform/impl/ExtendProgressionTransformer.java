package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Extend Progression – repeat entire progression twice.
 * [[2,5,9],[7,11,14],[0,4,7]] →
 * [[2,5,9],[7,11,14],[0,4,7],[2,5,9],[7,11,14],[0,4,7]]
 */
public class ExtendProgressionTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int len = orig.length;
        int[][] out = new int[len * 2][];
        for (int i = 0; i < len; i++) {
            out[i] = orig[i];
            out[i + len] = orig[i];
        }
        return new Cadence(
            "Extended " + c.type(),
            out,
            null,
            "Extend Progression – progression repeated twice"
        );
    }
}
