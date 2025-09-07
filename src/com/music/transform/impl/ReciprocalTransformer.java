package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Reciprocal – replace each semitone offset x with:
 *   x' = 12 * floorDiv(x, 12) + ((12 - floorMod(x, 12)) % 12)
 *
 * This transforms only the pitch class and preserves the octave.
 * Examples:
 *  - [2, 5, 9]      -> [10, 7, 3]
 *  - [0, 12, 24]    -> [0, 12, 24]   (octaves respected)
 *  - [-1, 11, 13]   -> [-11, 1, 11]  (negatives handled)
 */
public class ReciprocalTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] in  = c.intervals();
        int[][] out = new int[in.length][];

        for (int i = 0; i < in.length; i++) {
            out[i] = new int[in[i].length];
            for (int j = 0; j < in[i].length; j++) {
                int x   = in[i][j];
                int oct = Math.floorDiv(x, 12);
                int pc  = Math.floorMod(x, 12);          // 0..11
                int rpc = (12 - pc) % 12;                // reciprocal pitch class, 0..11
                out[i][j] = oct * 12 + rpc;              // preserve octave
            }
        }

        return new Cadence(
            "Reciprocal of " + c.type(),
            out,
            null,
            "Reciprocal – (12 − pc)"
        );
    }
}
