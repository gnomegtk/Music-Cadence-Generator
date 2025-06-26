package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Major→Minor – lower the 3rd by 1 semitone.
 * E.g. [7,11,14] → [7,10,14]
 */
public class MajorToMinorTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] out  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            out[i] = orig[i].clone();
            if (out[i].length > 1) out[i][1] = out[i][1] - 1;
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, out);
        return new Cadence(
            "Minorized " + c.type(),
            out,
            notes,
            "Major→Minor – lower the 3rd, e.g. [7,11,14] → [7,10,14]"
        );
    }
}
