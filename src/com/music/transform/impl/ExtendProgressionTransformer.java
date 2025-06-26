package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Extend Progression – repeat the entire progression twice.
 * E.g. [[2,5,9],[7,11,14],[0,4,7]] →
 *      [[2,5,9],[7,11,14],[0,4,7],[2,5,9],[7,11,14],[0,4,7]]
 */
public class ExtendProgressionTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] ext  = new int[orig.length * 2][];
        for (int i = 0; i < orig.length; i++) {
            ext[i]               = orig[i];
            ext[i + orig.length] = orig[i];
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, ext);
        return new Cadence(
            "Extended " + c.type(),
            ext,
            notes,
            "Extend Progression – e.g. progression repeated twice"
        );
    }
}
