package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Duplicate Chords – duplicate each chord’s intervals.
 * E.g. [2,5,9] → [2,5,9,2,5,9]
 */
public class DuplicateChordTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] dup  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            int[] row = orig[i];
            dup[i] = new int[row.length * 2];
            for (int j = 0; j < row.length; j++) {
                dup[i][j]             = row[j];
                dup[i][j + row.length] = row[j];
            }
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, dup);
        return new Cadence(
            "Duplicate Chords of " + c.type(),
            dup,
            notes,
            "Duplicate Chords – e.g. [2,5,9] → [2,5,9,2,5,9]"
        );
    }
}
