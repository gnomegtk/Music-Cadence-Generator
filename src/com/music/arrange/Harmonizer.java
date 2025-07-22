package com.music.arrange;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

import java.util.Arrays;

/**
 * Harmonizer distributing triads into SATB ranges,
 * with special penultimate/final rules, smooth voice leading,
 * and enforced spacing between voices (≤ one octave apart).
 *
 * Voice ranges (MIDI):
 *   Bass:    40–60
 *   Tenor:   48–67
 *   Alto:    55–74
 *   Soprano: 60–81
 */
public class Harmonizer implements Transformer {

    private static final int BASS_MIN    = 40, BASS_MAX    = 60;
    private static final int TENOR_MIN   = 48, TENOR_MAX   = 67;
    private static final int ALTO_MIN    = 55, ALTO_MAX    = 74;
    private static final int SOPRANO_MIN = 60, SOPRANO_MAX = 81;

    @Override
    public Cadence transform(Cadence input) {
        int[][] src   = input.intervals();
        int     total = src.length;
        int[][] out   = new int[total][];
        int[]   prev  = null;

        for (int i = 0; i < total; i++) {
            // sort triad offsets [root, third, fifth]
            int[] triad = Arrays.copyOf(src[i], src[i].length);
            Arrays.sort(triad);

            // build SATB pitches (absolute MIDI)
            int[] chord4 = buildSATB(triad, i, total);

            // smooth voice leading on all but penult/final
            if (prev != null && i < total - 2) {
                chord4 = voiceLead(prev, chord4);
            }

            // enforce ≤1-octave spacing between adjacent voices
            chord4 = enforceSpacing(chord4);

            // record for next
            Arrays.sort(chord4);
            prev = Arrays.copyOf(chord4, chord4.length);

            out[i] = chord4;
        }

        return new Cadence(input.type(), out, null, input.description());
    }

    /**
     * Assigns triad tones to SATB, applies special rules,
     * and fits into each voice’s range.
     */
    private int[] buildSATB(int[] triad, int index, int total) {
        int rootMidi  = 60 + triad[0];
        int thirdMidi = 60 + triad[1];
        int fifthMidi = 60 + triad[2];

        int bass, tenor, alto, soprano;

        // Penultimate: duplicate the third an octave up
        if (index == total - 2) {
            bass    = rootMidi;
            tenor   = thirdMidi;
            alto    = fifthMidi;
            soprano = thirdMidi + 12;

        // Final: SATB root-position (root, 3rd, 5th, root+12)
        } else if (index == total - 1) {
            bass    = rootMidi;
            tenor   = rootMidi + 4;
            alto    = rootMidi + 7;
            soprano = rootMidi + 12;

        // Default: root, third, fifth, root+12
        } else {
            bass    = rootMidi;
            tenor   = thirdMidi;
            alto    = fifthMidi;
            soprano = rootMidi + 12;
        }

        // Fit each voice into its range
        bass    = fitToRange(bass,    BASS_MIN,    BASS_MAX);
        tenor   = fitToRange(tenor,   TENOR_MIN,   TENOR_MAX);
        alto    = fitToRange(alto,    ALTO_MIN,    ALTO_MAX);
        soprano = fitToRange(soprano, SOPRANO_MIN, SOPRANO_MAX);

        return new int[]{ bass, tenor, alto, soprano };
    }

    /** Shift pitch by octaves to fall within [min,max]. */
    private int fitToRange(int pitch, int min, int max) {
        while (pitch < min)   pitch += 12;
        while (pitch > max)   pitch -= 12;
        return pitch;
    }

    /**
     * Voice leading: shift each voice by –12/0/+12 semitones
     * to minimize motion from corresponding voice in prev.
     */
    private int[] voiceLead(int[] prev, int[] curr) {
        int n = curr.length;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            out[i] = shiftNearest(curr[i], prev[i]);
        }
        return out;
    }

    /** Pick shift of –12,0,+12 to get closest to target. */
    private int shiftNearest(int orig, int target) {
        int best     = orig;
        int bestDiff = Math.abs(orig - target);
        for (int shift : new int[]{ -12, +12 }) {
            int cand = orig + shift;
            int diff = Math.abs(cand - target);
            if (diff < bestDiff) {
                bestDiff = diff;
                best     = cand;
            }
        }
        return best;
    }

    /**
     * Enforce no more than 12 semitones between adjacent voices:
     * tries lowering the upper voice by an octave if possible,
     * otherwise raising the lower voice by an octave.
     */
    private int[] enforceSpacing(int[] voices) {
        // voices order: [bass,tenor,alto,soprano]
        int[] mins = { BASS_MIN, TENOR_MIN, ALTO_MIN, SOPRANO_MIN };
        int[] maxs = { BASS_MAX, TENOR_MAX, ALTO_MAX, SOPRANO_MAX };

        for (int i = 0; i < 3; i++) {
            int lower = voices[i], upper = voices[i+1];
            int gap = upper - lower;
            if (gap > 12) {
                // try lower upper by octave
                if (upper - 12 >= mins[i+1]) {
                    voices[i+1] = upper - 12;
                }
                // else try raising lower voice
                else if (lower + 12 <= maxs[i]) {
                    voices[i] = lower + 12;
                }
            }
        }
        return voices;
    }
}
