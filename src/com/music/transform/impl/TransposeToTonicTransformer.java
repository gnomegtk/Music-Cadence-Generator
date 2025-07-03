package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Automatically transposes a Cadence to match a given tonic,
 * by shifting all intervals relative to the selected tonic.
 * This transformer should not appear in the UI combo box.
 */
public class TransposeToTonicTransformer implements Transformer {

    @Override
    public Cadence transform(Cadence input, String tonic) {
        if (tonic == null || tonic.equalsIgnoreCase(input.tonic())) {
            return input;
        }

        int from = input.getTonicMidi();
        int to   = new Cadence("x", "", tonic, null, null).getTonicMidi();
        int diff = to - from;

        int[][] original = input.intervals();
        int[][] shifted  = new int[original.length][];

        for (int i = 0; i < original.length; i++) {
            shifted[i] = new int[original[i].length];
            for (int j = 0; j < original[i].length; j++) {
                shifted[i][j] = original[i][j] + diff;
            }
        }

        return new Cadence(
            input.type(),
            input.description(),
            tonic,
            shifted,
            null
        );
    }
}
