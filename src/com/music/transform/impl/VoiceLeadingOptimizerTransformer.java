package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

import java.util.*;

/**
 * VoiceLeadingOptimizerTransformer
 *
 * Applies traditional voice leading rules to each chord transition:
 *  - Avoid parallel fifths and octaves between any pair of voices
 *  - Prefer contrary motion between bass (lowest voice) and soprano (highest voice)
 *  - Avoid voice crossing and keep voices within reasonable tessitura
 *  - Penalize large leaps; prefer stepwise motion
 *  - Penalize repeating the same pitch class in the same voice when alternatives exist
 *
 * Algorithm:
 *  1. For each transition, generate all permutations of the target chord's notes (same size as previous chord).
 *  2. Filter permutations that violate hard rules (parallels, crossing, tessitura).
 *  3. Score remaining permutations based on movement and stylistic preferences.
 *  4. Choose the lowest-scoring candidate.
 *
 * Assumptions:
 *  - Input chords are arrays of absolute pitches (e.g., MIDI numbers).
 *  - The lowest index (0) is treated as the bass; highest index as the soprano.
 *  - Tessitura ranges are generic defaults; adjust as needed.
 */
public class VoiceLeadingOptimizerTransformer implements Transformer {

    // Generic tessitura ranges for SATB-like spacing; adjust to your repertoire
    private static final int[] MIN_TESSITURA = new int[]{40, 48, 55, 60}; // Bass, Tenor, Alto, Soprano
    private static final int[] MAX_TESSITURA = new int[]{58, 67, 74, 84};

    @Override
    public Cadence transform(Cadence c) {
        int[][] chords = c.intervals();
        if (chords == null || chords.length == 0) return c;

        List<int[]> optimized = new ArrayList<>();
        optimized.add(chords[0].clone()); // keep first chord

        for (int i = 1; i < chords.length; i++) {
            int[] prev = optimized.get(i - 1);
            int[] next = chords[i];

            // If chord sizes differ, normalize by truncating or duplicating closest voices
            int[] adjustedNext = alignVoices(next, prev.length);

            int[] best = chooseBestVoiceLeading(prev, adjustedNext);
            optimized.add(best);
        }

        return new Cadence(
            "Voice Leading Optimized " + c.type(),
            optimized.toArray(new int[0][]),
            null,
            "Voice Leading Optimization – traditional contrapuntal rules applied"
        );
    }

    private int[] alignVoices(int[] chord, int targetSize) {
        if (chord.length == targetSize) return chord.clone();
        int[] out = new int[targetSize];
        if (chord.length > targetSize) {
            // Truncate by removing middle voices first
            int[] copy = chord.clone();
            Arrays.sort(copy);
            for (int i = 0; i < targetSize; i++) {
                out[i] = copy[i + (copy.length - targetSize)];
            }
            return out;
        } else {
            // Duplicate closest pitches to reach target size
            int[] copy = chord.clone();
            Arrays.sort(copy);
            for (int i = 0; i < targetSize; i++) {
                out[i] = copy[Math.min(i, copy.length - 1)];
            }
            return out;
        }
    }

    private int[] chooseBestVoiceLeading(int[] prev, int[] next) {
        List<int[]> candidates = generatePermutations(next);
        int[] best = next.clone();
        int bestScore = Integer.MAX_VALUE;

        for (int[] cand : candidates) {
            if (violatesHardRules(prev, cand)) continue;
            int score = scorePermutation(prev, cand);
            if (score < bestScore) {
                bestScore = score;
                best = cand.clone();
            }
        }
        return best;
    }

    /**
     * Hard rules: parallel fifths/octaves, voice crossing, tessitura.
     */
    private boolean violatesHardRules(int[] prev, int[] cand) {
        // Parallel fifths/octaves
        if (hasParallelFifthOrOctave(prev, cand)) return true;

        // Voice crossing: enforce ascending order bass->soprano
        if (!isNonCrossing(cand)) return true;

        // Tessitura bounds (if chord is 4 voices; else relax)
        if (cand.length == 4 && !inTessitura(cand)) return true;

        return false;
    }

    private boolean hasParallelFifthOrOctave(int[] prev, int[] cand) {
        int n = Math.min(prev.length, cand.length);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int ip = ((prev[i] - prev[j]) % 12 + 12) % 12;
                int ic = ((cand[i] - cand[j]) % 12 + 12) % 12;
                if ((ip == 7 || ip == 0) && ip == ic) return true;
            }
        }
        return false;
    }

    private boolean isNonCrossing(int[] chord) {
        for (int i = 1; i < chord.length; i++) {
            if (chord[i] < chord[i - 1]) return false;
        }
        return true;
    }

    private boolean inTessitura(int[] chord) {
        int n = Math.min(chord.length, MIN_TESSITURA.length);
        for (int i = 0; i < n; i++) {
            if (chord[i] < MIN_TESSITURA[i] || chord[i] > MAX_TESSITURA[i]) return false;
        }
        return true;
    }

    /**
     * Soft preferences scored: movement, contrary motion, large leaps, same-pc repetition.
     * Lower score is better.
     */
    private int scorePermutation(int[] prev, int[] cand) {
        int score = 0;

        // Movement distance and leap penalties
        for (int i = 0; i < prev.length; i++) {
            int diff = Math.abs(prev[i] - cand[i]);
            score += diff;
            if (diff > 9) score += 6;   // penalize leaps > major sixth
            else if (diff > 7) score += 4; // penalize leaps > perfect fifth
        }

        // Prefer contrary motion between bass and soprano
        int bassDir = Integer.compare(cand[0], prev[0]);
        int soprDir = Integer.compare(cand[prev.length - 1], prev[prev.length - 1]);
        if (bassDir == soprDir && bassDir != 0) {
            score += 5;
        }

        // Penalize same pitch class repetition in the same voice when alternatives exist
        for (int i = 0; i < prev.length; i++) {
            if (mod12(prev[i]) == mod12(cand[i])) {
                score += 3;
            }
        }

        // Encourage spacing (avoid too close clustering in upper voices)
        for (int i = 1; i < cand.length; i++) {
            int span = cand[i] - cand[i - 1];
            if (span < 3) score += 2; // penalize semitone-whole-tone clustering
        }

        return score;
    }

    private int mod12(int p) {
        int m = p % 12;
        return m < 0 ? m + 12 : m;
    }

    private List<int[]> generatePermutations(int[] notes) {
        // Ensure ascending order to start from a non-crossing base
        int[] sorted = notes.clone();
        Arrays.sort(sorted);
        List<int[]> perms = new ArrayList<>();
        permute(sorted, 0, perms);
        // Filter duplicates if chord has repeated pitches
        return unique(perms);
    }

    private void permute(int[] arr, int k, List<int[]> result) {
        if (k == arr.length) {
            result.add(arr.clone());
        } else {
            for (int i = k; i < arr.length; i++) {
                swap(arr, k, i);
                permute(arr, k + 1, result);
                swap(arr, k, i);
            }
        }
    }

    private void swap(int[] arr, int i, int j) {
        if (i == j) return;
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    private List<int[]> unique(List<int[]> perms) {
        List<int[]> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int[] p : perms) {
            String key = Arrays.toString(p);
            if (!seen.contains(key)) {
                seen.add(key);
                out.add(p);
            }
        }
        return out;
    }
}
