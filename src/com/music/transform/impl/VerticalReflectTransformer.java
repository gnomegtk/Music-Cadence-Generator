package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Vertical reflect – reverse each chord’s row of intervals.
 * [a,b,c] → [c,b,a]
 */
public class VerticalReflectTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] vr   = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            int[] row = orig[i];
            vr[i] = new int[row.length];
            for (int j = 0; j < row.length; j++) {
                vr[i][j] = row[row.length - 1 - j];
            }
        }
        // transpose back to Note[][]
        var notes = CadenceRegistry.transposeMatrix(tonic, vr);
        return new Cadence(
            "Vertically Reflected " + c.type(),
            vr,
            notes,
            "Rows mirrored (each chord’s intervals reversed)"
        );
    }
}
