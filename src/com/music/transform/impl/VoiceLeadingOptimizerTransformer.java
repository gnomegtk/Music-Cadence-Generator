package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;
import java.util.Arrays;

/**
 * Voice Leading Optimizer – reorders chord notes to minimize movement
 * between consecutive chords. Keeps the same pitch classes.
 */
public class VoiceLeadingOptimizerTransformer implements Transformer {

    private static int distance(int a, int b) { return Math.abs(a - b); }

    private static int[] optimizeFromPrev(int[] prev, int[] curr) {
        int[] currCopy = Arrays.copyOf(curr, curr.length);
        Arrays.sort(currCopy);
        int[] prevCopy = Arrays.copyOf(prev, prev.length);
        Arrays.sort(prevCopy);

        // Greedy: align each voice to the closest available note
        boolean[] used = new boolean[currCopy.length];
        int[] result = new int[currCopy.length];
        for (int i = 0; i < prevCopy.length; i++) {
            int bestJ = -1, bestDist = Integer.MAX_VALUE;
            for (int j = 0; j < currCopy.length; j++) {
                if (used[j]) continue;
                int d = distance(prevCopy[i], currCopy[j]);
                if (d < bestDist) { bestDist = d; bestJ = j; }
            }
            used[bestJ] = true;
            result[i] = currCopy[bestJ];
        }
        // Add remaining notes
        int idx = prevCopy.length;
        for (int j = 0; j < currCopy.length; j++) {
            if (!used[j]) result[idx++] = currCopy[j];
        }
        return result;
    }

    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        if (orig.length == 0) return c;

        int[][] out = new int[orig.length][];
        out[0] = Arrays.copyOf(orig[0], orig[0].length);
        for (int i = 1; i < orig.length; i++) {
            out[i] = optimizeFromPrev(out[i-1], orig[i]);
        }
        return new Cadence(
            "Voice Leading Optimized " + c.type(),
            out,
            null,
            "Voice Leading – reorders chord notes to minimize movement between chords"
        );
    }
}
