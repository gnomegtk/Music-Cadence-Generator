package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Vertical Reflect – reverse each chord’s intervals.
 * E.g. [a,b,c] → [c,b,a]
 */
public class VerticalReflectTransformer implements Transformer {

    @Override
    public Cadence transform(Cadence input) {
        int[][] orig = input.intervals();
        int[][] vr   = new int[orig.length][];

        for (int i = 0; i < orig.length; i++) {
            int[] chord = orig[i];
            vr[i] = new int[chord.length];
            for (int j = 0; j < chord.length; j++) {
                vr[i][j] = chord[chord.length - 1 - j];
            }
        }

        return new Cadence(
            "Vertically Reflected " + input.type(),
            vr,
            null,
            "Vertical reflect – reverse each chord’s intervals"
        );
    }
}
