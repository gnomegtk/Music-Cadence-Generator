// AddNinthTransformer.java
package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Add Ninth – append the 9th to each chord.
 * E.g. [0,4,7] → [0,4,7,14]
 */
public class AddNinthTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] ext  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            int[] row = orig[i];
            ext[i] = new int[row.length + 1];
            System.arraycopy(row, 0, ext[i], 0, row.length);
            ext[i][row.length] = row[0] + 14; // ninth = root + 14 semis
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, ext);
        return new Cadence(
            "Add Ninth to " + c.type(),
            ext,
            notes,
            "Add Ninth – e.g. [0,4,7] → [0,4,7,14]"
        );
    }
}
