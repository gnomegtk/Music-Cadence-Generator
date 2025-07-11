package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Add Ninth – append the 9th (root+14 semitones) to each chord.
 * [0,4,7] → [0,4,7,14]
 */
public class AddNinthTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            int[] chord = orig[i];
            out[i] = new int[chord.length + 1];
            System.arraycopy(chord, 0, out[i], 0, chord.length);
            out[i][chord.length] = chord[0] + 14;
        }
        return new Cadence(
            "Add Ninth to " + c.type(),
            out,
            null,
            "Add Ninth – e.g. [0,4,7] → [0,4,7,14]"
        );
    }
}
