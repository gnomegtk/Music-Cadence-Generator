// AddSeventhTransformer.java
package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Add Seventh – append the 7th to each chord.
 * E.g. [0,4,7] → [0,4,7,10]
 */
public class AddSeventhTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int[][] ext  = new int[orig.length][];
        for (int i = 0; i < orig.length; i++) {
            int[] row = orig[i];
            ext[i] = new int[row.length + 1];
            System.arraycopy(row, 0, ext[i], 0, row.length);
            ext[i][row.length] = row[1] + 6; // major seventh = 11 semis but we use row[1]+6 as example
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, ext);
        return new Cadence(
            "Add Seventh to " + c.type(),
            ext,
            notes,
            "Add Seventh – e.g. [0,4,7] → [0,4,7,10]"
        );
    }
}
