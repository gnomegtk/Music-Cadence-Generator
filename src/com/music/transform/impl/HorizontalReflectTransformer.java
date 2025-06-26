package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Horizontal reflect – reverse the time axis (row order).
 * [I,II,III] → [III,II,I]
 */
public class HorizontalReflectTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int    n    = orig.length;
        int[][] hr   = new int[n][];
        for (int i = 0; i < n; i++) {
            hr[i] = orig[n - 1 - i];
        }
        // transpose back to Note[][]
        var notes = CadenceRegistry.transposeMatrix(tonic, hr);
        return new Cadence(
            "Horizontally Reflected " + c.type(),
            hr,
            notes,
            "Chord sequence reversed (time axis mirrored)"
        );
    }
}
