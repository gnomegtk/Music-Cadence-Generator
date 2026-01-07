package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

import java.util.*;

/**
 * DodecafonizeTransformer
 *
 * Enforces twelve-tone usage across the progression:
 *  - No pitch class repeats until all 12 classes have been used.
 *  - Once a 12-tone row is completed, the next row is selected by comparing
 *    all Schoenberg-style transformations (P, R, I, RI and transpositions)
 *    to the upcoming musical segment and choosing the closest match.
 *  - Notes are processed in musical order but the original chord grid is preserved:
 *    we substitute pitch classes within each chord instead of collapsing to monophony.
 *
 * Assumptions:
 *  - Input matrices (int[][]) represent absolute pitches (e.g., MIDI numbers).
 *  - Pitch class is derived as (pitch % 12).
 *  - The transformer operates across the full texture (all voices together).
 */
public class DodecafonizeTransformer implements Transformer {

    // Lookahead length to compare candidate next row against upcoming segment
    private static final int LOOKAHEAD_NOTES = 6;

    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        if (orig == null || orig.length == 0) {
            return c;
        }

        // Flatten progression into a linear list of notes with positions
        List<NotePos> linear = flatten(orig);

        // Output grid preserving original chord/voice structure
        int[][] out = deepCopy(orig);

        // State for current row enforcement
        Set<Integer> used = new HashSet<>();
        List<Integer> currentRow = new ArrayList<>();
        List<Integer> lastCompletedRow = null;

        // If we complete a row, we will select the next row and iterate through it
        List<Integer> nextRow = null;
        int nextRowIndex = 0;

        for (int idx = 0; idx < linear.size(); idx++) {
            NotePos np = linear.get(idx);
            int targetPc = mod12(np.pitch);

            int chosenPc;

            // If we are within a selected nextRow, keep consuming its sequence
            if (nextRow != null && nextRowIndex < nextRow.size()) {
                chosenPc = nextRow.get(nextRowIndex);
                nextRowIndex++;

                // Avoid duplicate pitch class within the same chord (optional refinement)
                // If this chord already contains chosenPc, advance to the next unused from nextRow
                Set<Integer> chordPcs = chordPitchClasses(out[np.chordIndex]);
                if (chordPcs.contains(chosenPc)) {
                    int scan = nextRowIndex;
                    while (scan < nextRow.size() && chordPcs.contains(nextRow.get(scan))) {
                        scan++;
                    }
                    if (scan < nextRow.size()) {
                        chosenPc = nextRow.get(scan);
                        nextRowIndex = scan + 1;
                    }
                }

                // Update row state
                used.add(chosenPc);
                currentRow.add(chosenPc);

            } else {
                // Not currently following a preselected row: enforce no repeat until 12
                if (!used.contains(targetPc)) {
                    chosenPc = targetPc;
                } else {
                    // Choose the best unused pitch class close to target, avoiding duplicates within chord
                    Set<Integer> chordPcs = chordPitchClasses(out[np.chordIndex]);
                    chosenPc = chooseUnusedClosest(targetPc, used, chordPcs);
                }
                used.add(chosenPc);
                currentRow.add(chosenPc);
            }

            // Write chosen pitch class back into the output chord, preserving absolute octave as much as possible
            out[np.chordIndex][np.voiceIndex] = reassignToPitchClass(out[np.chordIndex][np.voiceIndex], chosenPc);

            // If we completed a 12-tone row, prepare next row selection
            if (currentRow.size() == 12) {
                lastCompletedRow = new ArrayList<>(currentRow);
                used.clear();
                currentRow.clear();

                // Choose next row by comparing candidates against the upcoming segment
                List<Integer> segment = collectLookaheadSegment(linear, idx + 1, LOOKAHEAD_NOTES);
                nextRow = chooseBestRow(lastCompletedRow, segment);
                nextRowIndex = 0;
            }
        }

        return new Cadence(
            "Dodecafonized " + c.type(),
            out,
            null,
            "Dodecafonize – no pitch-class repetition until 12 are used; next rows chosen by best-fitting Schoenberg transformation"
        );
    }

    // --- Helpers ---

    private static class NotePos {
        final int chordIndex;
        final int voiceIndex;
        final int pitch;
        NotePos(int ci, int vi, int p) { this.chordIndex = ci; this.voiceIndex = vi; this.pitch = p; }
    }

    private List<NotePos> flatten(int[][] grid) {
        List<NotePos> linear = new ArrayList<>();
        for (int ci = 0; ci < grid.length; ci++) {
            int[] chord = grid[ci];
            for (int vi = 0; vi < chord.length; vi++) {
                linear.add(new NotePos(ci, vi, chord[vi]));
            }
        }
        return linear;
    }

    private int[][] deepCopy(int[][] src) {
        int[][] dst = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            dst[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return dst;
    }

    private int mod12(int p) {
        int m = p % 12;
        return m < 0 ? m + 12 : m;
    }

    private Set<Integer> chordPitchClasses(int[] chord) {
        Set<Integer> pcs = new HashSet<>();
        for (int p : chord) pcs.add(mod12(p));
        return pcs;
    }

    /**
     * Choose the unused pitch class that is closest to the target,
     * while also avoiding duplication within the current chord if possible.
     */
    private int chooseUnusedClosest(int targetPc, Set<Integer> used, Set<Integer> chordPcs) {
        int bestPc = -1;
        int bestScore = Integer.MAX_VALUE;
        for (int pc = 0; pc < 12; pc++) {
            if (used.contains(pc)) continue;
            int dist = circularDistance(pc, targetPc);
            int penalty = (chordPcs != null && chordPcs.contains(pc)) ? 3 : 0;
            int score = dist + penalty;
            if (score < bestScore) {
                bestScore = score;
                bestPc = pc;
            }
        }
        return bestPc >= 0 ? bestPc : targetPc;
    }

    private int circularDistance(int a, int b) {
        int d = Math.abs(a - b);
        return Math.min(d, 12 - d);
    }

    /**
     * Reassign the given absolute pitch to the chosen pitch class,
     * preserving octave proximity (choose nearest octave for that pc).
     */
    private int reassignToPitchClass(int originalPitch, int pc) {
        int base = originalPitch - mod12(originalPitch) + pc;
        // Evaluate three octave positions: base-12, base, base+12 and choose the closest to originalPitch
        int best = base;
        int[] candidates = new int[]{base - 12, base, base + 12};
        int bestDist = Integer.MAX_VALUE;
        for (int cand : candidates) {
            int dist = Math.abs(cand - originalPitch);
            if (dist < bestDist) {
                bestDist = dist;
                best = cand;
            }
        }
        return best;
        }

    private List<Integer> collectLookaheadSegment(List<NotePos> linear, int startIdx, int count) {
        List<Integer> seg = new ArrayList<>();
        for (int i = startIdx; i < linear.size() && seg.size() < count; i++) {
            seg.add(mod12(linear.get(i).pitch));
        }
        return seg;
    }

    // --- Row transformations and selection ---

    private List<Integer> invert(List<Integer> row) {
        List<Integer> inv = new ArrayList<>();
        int axis = row.get(0); // invert around first pitch class
        for (int pc : row) {
            int interval = mod12(pc - axis);
            inv.add(mod12(axis - interval));
        }
        return inv;
    }

    private List<Integer> retrograde(List<Integer> row) {
        List<Integer> ret = new ArrayList<>(row);
        Collections.reverse(ret);
        return ret;
    }

    private List<Integer> transpose(List<Integer> row, int shift) {
        List<Integer> trans = new ArrayList<>(row.size());
        for (int pc : row) trans.add(mod12(pc + shift));
        return trans;
    }

    /**
     * Choose the next row transformation that best matches the upcoming segment.
     * Candidates: P, R, I, RI, and transpositions of P (1..11).
     * Tie-breaker preference: P > R > I > RI > smallest transposition.
     */
    private List<Integer> chooseBestRow(List<Integer> baseRow, List<Integer> segment) {
        List<List<Integer>> candidates = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // P
        candidates.add(baseRow);
        labels.add("P");
        // R
        candidates.add(retrograde(baseRow));
        labels.add("R");
        // I
        candidates.add(invert(baseRow));
        labels.add("I");
        // RI
        candidates.add(retrograde(invert(baseRow)));
        labels.add("RI");
        // Transpositions of P
        for (int t = 1; t < 12; t++) {
            candidates.add(transpose(baseRow, t));
            labels.add("P+" + t);
        }

        int bestScore = Integer.MAX_VALUE;
        int bestIdx = 0;

        for (int i = 0; i < candidates.size(); i++) {
            int score = distanceScore(candidates.get(i), segment);
            if (score < bestScore) {
                bestScore = score;
                bestIdx = i;
            } else if (score == bestScore) {
                // Tie-breaker by label order preference
                if (prefer(labels.get(i), labels.get(bestIdx))) {
                    bestIdx = i;
                }
            }
        }
        return candidates.get(bestIdx);
    }

    private boolean prefer(String a, String b) {
        // Preference order: P, R, I, RI, then smaller transposition
        int rankA = rankLabel(a);
        int rankB = rankLabel(b);
        if (rankA != rankB) return rankA < rankB;
        // If both are transpositions, prefer smaller shift
        if (a.startsWith("P+") && b.startsWith("P+")) {
            int ta = Integer.parseInt(a.substring(2));
            int tb = Integer.parseInt(b.substring(2));
            return ta < tb;
        }
        return false;
    }

    private int rankLabel(String label) {
        if ("P".equals(label)) return 0;
        if ("R".equals(label)) return 1;
        if ("I".equals(label)) return 2;
        if ("RI".equals(label)) return 3;
        if (label.startsWith("P+")) return 4;
        return 5;
    }

    private int distanceScore(List<Integer> candidate, List<Integer> segment) {
        int len = Math.min(candidate.size(), segment.size());
        if (len == 0) return Integer.MAX_VALUE / 2;
        int sum = 0;
        for (int i = 0; i < len; i++) {
            int a = candidate.get(i);
            int b = segment.get(i);
            sum += circularDistance(a, b);
        }
        return sum;
    }
}
