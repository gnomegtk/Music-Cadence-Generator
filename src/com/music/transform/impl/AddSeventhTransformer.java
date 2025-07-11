package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Add Seventh – append the 7th to each chord.
 * [0,4,7] → [0,4,7,10]
 */
public class AddSeventhTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            int[] chord = orig[i];
            out[i] = new int[chord.length + 1];
            System.arraycopy(chord, 0, out[i], 0, chord.length);
            // example: minor seventh = second degree +6
            out[i][chord.length] = chord[1] + 6;
        }
        return new Cadence(
            "Add Seventh to " + c.type(),
            out,
            null,
            "Add Seventh – e.g. [0,4,7] → [0,4,7,10]"
        );
    }
}
