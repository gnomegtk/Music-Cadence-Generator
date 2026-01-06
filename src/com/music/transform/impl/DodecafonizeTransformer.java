package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

import java.util.*;

/**
 * Dodecafonize – transforms a cadence into a 12-tone row.
 * Ensures each of the 12 pitch classes appears only once before repetition.
 * If needed, adjusts notes to nearest unused pitch class.
 * At the end, completes the row according to Schoenberg's 12-tone technique.
 */
public class DodecafonizeTransformer implements Transformer {

    @Override
    public Cadence transform(Cadence c) {
        int[][] orig = c.intervals();
        List<Integer> row = new ArrayList<>();
        Set<Integer> used = new HashSet<>();

        // Build row from cadence chords
        for (int[] chord : orig) {
            for (int note : chord) {
                int pc = (note % 12 + 12) % 12;
                if (!used.contains(pc)) {
                    row.add(pc);
                    used.add(pc);
                } else {
                    // If already used, adjust to nearest unused pitch class
                    int adjusted = findNearestUnused(pc, used);
                    row.add(adjusted);
                    used.add(adjusted);
                }
                if (row.size() == 12) break;
            }
            if (row.size() == 12) break;
        }

        // Complete row if not full
        for (int pc = 0; row.size() < 12 && pc < 12; pc++) {
            if (!used.contains(pc)) {
                row.add(pc);
                used.add(pc);
            }
        }

        // Convert row into one chord per note (monophonic series)
        int[][] out = new int[row.size()][1];
        for (int i = 0; i < row.size(); i++) {
            out[i][0] = row.get(i);
        }

        return new Cadence(
            "Dodecafonized " + c.type(),
            out,
            null,
            "Dodecafonize – transforms cadence into a 12-tone row (Schoenberg technique)"
        );
    }

    private int findNearestUnused(int pc, Set<Integer> used) {
        for (int dist = 1; dist < 12; dist++) {
            int up = (pc + dist) % 12;
            int down = (pc - dist + 12) % 12;
            if (!used.contains(up)) return up;
            if (!used.contains(down)) return down;
        }
        return pc; // fallback
    }
}
