package com.music.arrange;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

import java.util.*;

public class Harmonizer implements Transformer {

    private static final int BASS_MIN    = 40, BASS_MAX    = 60;
    private static final int TENOR_MIN   = 48, TENOR_MAX   = 67;
    private static final int ALTO_MIN    = 55, ALTO_MAX    = 74;
    private static final int SOPRANO_MIN = 60, SOPRANO_MAX = 81;
    private static final Random RNG      = new Random();

    @Override
    public Cadence transform(Cadence input) {
        int[][] semisIn       = input.intervals();
        int     totalChords   = semisIn.length;
        int     fixedBassCount= totalChords;                // usa o length da cadência
        int[][] midiGrid      = new int[totalChords][4];
        int[]   prevMidi      = null;

        for (int i = 0; i < totalChords; i++) {
            // 1) dedupe & sort semitons do acorde
            int[] tones = Arrays.stream(semisIn[i]).distinct().toArray();
            Arrays.sort(tones);

            // 2) gerar & embaralhar combinações SATB
            List<int[]> combos = generateVoiceCombinations(tones);
            Collections.shuffle(combos, RNG);

            // 3) filtrar voicings válidos
            List<int[]> valid = new ArrayList<>();
            for (int[] combo : combos) {
                if (combo.length != 4) continue;

                // 4) fixa a raiz no baixo nos últimos fixedBassCount acordes
                if (i >= totalChords - fixedBassCount && combo[0] != tones[0]) {
                    continue;
                }

                // 5) encaixa cada voz na sua tessitura
                int b = fitToRange(combo[0], BASS_MIN,    BASS_MAX);
                int t = fitToRange(combo[1], TENOR_MIN,   TENOR_MAX);
                int a = fitToRange(combo[2], ALTO_MIN,    ALTO_MAX);
                int s = fitToRange(combo[3], SOPRANO_MIN, SOPRANO_MAX);
                int[] midi = { b + 60, t + 60, a + 60, s + 60 };

                // 6) aplicar restrições musicais
                if (!allUnique(midi))                      continue;
                if (countInterval(midi, 12) > 1)           continue;
                if (countInterval(midi, 7)  > 1)           continue;
                if (!bassTenorWithinFifth(midi))           continue;
                if (!noVoiceSpacingExceeds(midi, 12))      continue;
                if (prevMidi != null && hasParallelFifth(prevMidi, midi)) {
                    continue;
                }

                valid.add(new int[]{ b, t, a, s });
            }

            // 7) escolhe voicing válido ou fallback
            int[] chosenOffsets = valid.isEmpty()
                ? dynamicFallback(tones, prevMidi)
                : valid.get(RNG.nextInt(valid.size()));

            // 8) converte offsets → MIDI e força espaçamento
            int[] midi = new int[4];
            for (int v = 0; v < 4; v++) {
                midi[v] = chosenOffsets[v] + 60;
            }
            enforceSpacingMidi(midi);

            midiGrid[i] = midi;
            prevMidi    = midi;
        }

        return new Cadence(input.type(), midiGrid, null, input.description());
    }

    private List<int[]> generateVoiceCombinations(int[] tones) {
        List<int[]> out = new ArrayList<>();
        if (tones.length >= 4) {
            out.addAll(permutations(Arrays.asList(
                tones[0], tones[1], tones[2], tones[3]
            )));
        } else {
            for (int t0 : tones) {
                List<Integer> combo = new ArrayList<>();
                for (int x : tones) combo.add(x);
                combo.add(t0 + 12);
                out.addAll(permutations(combo));
            }
        }
        return out;
    }

    private List<int[]> permutations(List<Integer> list) {
        List<int[]> acc = new ArrayList<>();
        permuteHelper(list, 0, acc);
        return acc;
    }

    private void permuteHelper(List<Integer> list, int idx, List<int[]> acc) {
        if (idx == list.size()) {
            acc.add(list.stream().mapToInt(x -> x).toArray());
            return;
        }
        for (int j = idx; j < list.size(); j++) {
            Collections.swap(list, idx, j);
            permuteHelper(list, idx + 1, acc);
            Collections.swap(list, idx, j);
        }
    }

    private int fitToRange(int offset, int min, int max) {
        int midi = offset + 60;
        while (midi < min) { offset += 12; midi += 12; }
        while (midi > max) { offset -= 12; midi -= 12; }
        return offset;
    }

    private boolean allUnique(int[] p) {
        Set<Integer> seen = new HashSet<>();
        for (int x : p) if (!seen.add(x)) return false;
        return true;
    }

    private int countInterval(int[] p, int sem) {
        int c = 0;
        for (int i = 0; i < p.length; i++) {
            for (int j = i + 1; j < p.length; j++) {
                if (Math.abs(p[j] - p[i]) == sem) c++;
            }
        }
        return c;
    }

    private boolean bassTenorWithinFifth(int[] p) {
        return Math.abs(p[1] - p[0]) <= 7;
    }

    private boolean noVoiceSpacingExceeds(int[] p, int max) {
        for (int i = 0; i < p.length - 1; i++) {
            if (p[i + 1] - p[i] > max) return false;
        }
        return true;
    }

    private boolean hasParallelFifth(int[] prev, int[] curr) {
        for (int v = 1; v < 4; v++) {
            int d1 = prev[v] - prev[0], d2 = curr[v] - curr[0];
            if (Math.abs(d1) == 7 && Math.abs(d2) == 7
             && (curr[0] - prev[0]) * (curr[v] - prev[v]) > 0) {
                return true;
            }
        }
        return false;
    }

    private int[] dynamicFallback(int[] tones, int[] prev) {
        int r = tones[0];
        int t = tones.length > 1 ? tones[1] : r + 4;
        int f = tones.length > 2 ? tones[2] : r + 7;

        int[][] cands = {
            {r, t, f, r + 12},
            {r, t, f, t + 12},
            {r, t, f, f + 12}
        };
        for (int[] c : cands) {
            int b  = fitToRange(c[0], BASS_MIN,    BASS_MAX);
            int tn = fitToRange(c[1], TENOR_MIN,   TENOR_MAX);
            int al = fitToRange(c[2], ALTO_MIN,    ALTO_MAX);
            int sp = fitToRange(c[3], SOPRANO_MIN, SOPRANO_MAX);
            int[] midi = {b + 60, tn + 60, al + 60, sp + 60};

            if (allUnique(midi)
             && countInterval(midi, 12) <= 1
             && countInterval(midi, 7)  <= 1
             && bassTenorWithinFifth(midi)
             && noVoiceSpacingExceeds(midi, 12)
             && (prev == null || !hasParallelFifth(prev, midi))) {
                return new int[]{b, tn, al, sp};
            }
        }

        // final fallback: root–3rd–5th–root+12
        return new int[]{
            fitToRange(tones[0],         BASS_MIN,    BASS_MAX),
            fitToRange(tones.length>1 ? tones[1] : tones[0]+4, TENOR_MIN,   TENOR_MAX),
            fitToRange(tones.length>2 ? tones[2] : tones[0]+7, ALTO_MIN,    ALTO_MAX),
            fitToRange(tones[0] + 12,    SOPRANO_MIN, SOPRANO_MAX)
        };
    }

    /**
     * Iteratively lower any voice that is more than an octave above its
     * lower neighbor until all adjacent intervals ≤ 12 semitones.
     */
    private void enforceSpacingMidi(int[] midi) {
        boolean changed;
        do {
            changed = false;
            for (int i = 0; i < midi.length - 1; i++) {
                if (midi[i + 1] - midi[i] > 12) {
                    midi[i + 1] -= 12;
                    changed = true;
                }
            }
        } while (changed);
    }
}
