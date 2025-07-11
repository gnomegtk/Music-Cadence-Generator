package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Horizontal Reflect – reverse the chord sequence.
 * E.g. [[2,5,9],[7,11,14],[0,4,7]] →
 *      [[0,4,7],[7,11,14],[2,5,9]]
 */
public class HorizontalReflectTransformer implements Transformer {

    @Override
    public Cadence transform(Cadence input) {
        int[][] orig = input.intervals();
        int n = orig.length;
        int[][] hr = new int[n][];

        for (int i = 0; i < n; i++) {
            hr[i] = orig[n - 1 - i];
        }

        return new Cadence(
            "Horizontally Reflected " + input.type(),
            hr,
            null,
            "Horizontal reflect – reverse chord sequence"
        );
    }
}
