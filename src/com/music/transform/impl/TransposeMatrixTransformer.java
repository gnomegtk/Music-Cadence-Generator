package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.registry.CadenceRegistry;
import com.music.transform.Transformer;

/**
 * Transpose Rows⇄Cols – swap rows and columns of the interval matrix.
 * E.g. [[2,5,9],[7,11,14],[0,4,7]] →
 *      [[2,7,0],[5,11,4],[9,14,7]]
 */
public class TransposeMatrixTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String tonic) {
        int[][] orig = c.intervals();
        int rows = orig.length;
        int cols = orig[0].length;
        int[][] t = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                t[j][i] = orig[i][j];
            }
        }
        var notes = CadenceRegistry.transposeMatrix(tonic, t);
        return new Cadence(
            "Transposed Rows⇄Cols of " + c.type(),
            t,
            notes,
            "Transpose Rows⇄Cols – e.g. [[2,5,9],[7,11,14],[0,4,7]] → [[2,7,0],[5,11,4],[9,14,7]]"
        );
    }
}
