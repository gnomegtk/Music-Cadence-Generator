package com.music.arrange;

import com.music.domain.Cadence;
import com.music.transform.Transformer;
import java.util.Arrays;

/**
 * Anchors a semitone grid into middle-C octave by adding 60.
 * Only this class cares that C4=60. Outputs pure MIDI grids.
 */
public class Harmonizer implements Transformer {

    public Harmonizer() { }

    public Cadence transform(Cadence input) {
        int[][] src = input.intervals();
        int[][] dst = new int[src.length][];

        for (int i = 0; i < src.length; i++) {
            dst[i] = new int[src[i].length];

            for (int j = 0; j < src[i].length; j++) {
                dst[i][j] = 60 + src[i][j];
            }
        }

        return new Cadence(input.type(), dst, null, input.description());
    }
}
