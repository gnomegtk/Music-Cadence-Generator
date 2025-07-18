package com.music.arrange;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

import java.util.Arrays;

/**
 * Harmonizer with special penultimate and final logic:
 * - Default: complete triads to [root, third, fifth, root+12], minimal voice leading.
 * - Penultimate: [root, third, fifth, third+12], skip voice leading.
 * - Final:   root-position SATB (C4–E4–G4–C5), skip voice leading.
 */
public class Harmonizer implements Transformer {

    @Override
    public Cadence transform(Cadence input) {
        int[][] src   = input.intervals();
        int     total = src.length;
        int[][] out   = new int[total][];
        int[]   prev  = null;

        for (int i = 0; i < total; i++) {
            // Sort input
            int[] triad = Arrays.copyOf(src[i], src[i].length);
            Arrays.sort(triad);

            int[] chord4;
            if (triad.length >= 4) {
                chord4 = triad;
            } else if (i == total - 2) {
                chord4 = duplicateThirdOctaveUp(triad);
            } else if (i == total - 1) {
                chord4 = finalSATB(triad[0]);
            } else {
                chord4 = duplicateRootOctaveUp(triad);
            }

            // Voice leading (skip penultimate & final)
            if (prev != null && i < total - 2) {
                chord4 = voiceLead(prev, chord4);
            }

            prev = Arrays.copyOf(chord4, chord4.length);

            // Anchor at C4=MIDI60
            int[] midi = new int[chord4.length];
            for (int j = 0; j < chord4.length; j++) {
                midi[j] = chord4[j] + 60;
            }
            out[i] = midi;
        }

        return new Cadence(input.type(), out, null, input.description());
    }

    // root, third, fifth, root+12
    private int[] duplicateRootOctaveUp(int[] t) {
        return new int[]{ t[0], t[1], t[2], t[0] + 12 };
    }

    // root, third, fifth, third+12
    private int[] duplicateThirdOctaveUp(int[] t) {
        return new int[]{ t[0], t[1], t[2], t[1] + 12 };
    }

    // final chord in SATB: root, third, fifth, root+12
    private int[] finalSATB(int root) {
        return new int[]{ root, root+4, root+7, root+12 };
    }

    // minimal ±12 shifting
    private int[] voiceLead(int[] prev, int[] curr) {
        int n = curr.length;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = shift(curr[i], prev[i]);
        }
        Arrays.sort(out);
        return out;
    }

    private int shift(int orig, int target) {
        int best = orig, diff = Math.abs(orig - target);
        for (int s : new int[]{-12,12}) {
            int cand = orig + s;
            int d    = Math.abs(cand - target);
            if (d < diff) { diff = d; best = cand; }
        }
        return best;
    }
}
