package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Major→Minor – lower the 3rd by one semitone.
 * [7,11,14] → [7,10,14]
 */
public class MajorToMinorTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = orig[i].clone();
            if (out[i].length > 1) out[i][1]--;
        }
        return new Cadence(
            "Minorized " + c.type(),
            out,
            null,
            "Major→Minor – lower the third"
        );
    }
}
