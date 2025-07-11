package com.music.util;

import com.music.domain.Note;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;

/**
 * Spells absolute MIDI pitches into Note(step, alter, octave),
 * and supplies MusicXML key‐signature fifths.
 *
 * Only Harmonizer ever knows about middle‐C = 60.
 * This helper treats all input numbers as absolute MIDI.
 */
public class KeySignatureHelper {

    // Universal pitch‐class labels (0=C, 1=C♯, … 11=B)
    private static final String[] PC_LABELS = {
        "C", "C♯", "D", "D♯", "E",
        "F", "F♯", "G", "G♯", "A",
        "A♯", "B"
    };

    // Key‐signature fifths for MusicXML
    private static final Map<String,Integer> TONIC_TO_FIFTHS = new LinkedHashMap<>();
    static {
        TONIC_TO_FIFTHS.put("C",  0);
        TONIC_TO_FIFTHS.put("G",  1);
        TONIC_TO_FIFTHS.put("D",  2);
        TONIC_TO_FIFTHS.put("A",  3);
        TONIC_TO_FIFTHS.put("E",  4);
        TONIC_TO_FIFTHS.put("B",  5);
        TONIC_TO_FIFTHS.put("F#", 6);
        TONIC_TO_FIFTHS.put("C#", 7);
        TONIC_TO_FIFTHS.put("F", -1);
        TONIC_TO_FIFTHS.put("Bb",-2);
        TONIC_TO_FIFTHS.put("Eb",-3);
        TONIC_TO_FIFTHS.put("Ab",-4);
        TONIC_TO_FIFTHS.put("Db",-5);
        TONIC_TO_FIFTHS.put("Gb",-6);
        TONIC_TO_FIFTHS.put("Cb",-7);
    }

    /**
     * Spell one absolute MIDI value into a Note.
     * The 'tonic' parameter is ignored here.
     */
    public static Note midiToNote(int midi, String tonic) {
        int pc     = (midi % 12 + 12) % 12;
        int octave = midi / 12 - 1;
        String label = PC_LABELS[pc];
        String step  = label.substring(0, 1);
        int alter    = label.length() > 1 ? 1 : 0;
        System.out.printf("DEBUG Spell: midi=%d pc=%d label=%s oct=%d%n",
                          midi, pc, label, octave);
        return new Note(step, alter, octave);
    }

    /**
     * Convert a grid of absolute MIDI pitches into Note[][].
     * Always uses midiToNote() above.
     */
    public static Note[][] computeMatrix(int[][] midiGrid, String tonic) {
        System.out.printf("=== DEBUG computeMatrix: key=%s ===%n", tonic);
        Note[][] out = new Note[midiGrid.length][];
        for (int i = 0; i < midiGrid.length; i++) {
            out[i] = new Note[midiGrid[i].length];
            System.out.printf("DEBUG computeMatrix: chord %d mids=%s%n",
                              i+1, Arrays.toString(midiGrid[i]));
            for (int j = 0; j < midiGrid[i].length; j++) {
                out[i][j] = midiToNote(midiGrid[i][j], tonic);
            }
        }
        return out;
    }

    /**
     * Returns the number of sharps (positive) or flats (negative)
     * for the given tonic’s key signature (MusicXML 'fifths').
     */
    public static int getKeySignatureFifths(String tonic) {
        return TONIC_TO_FIFTHS.getOrDefault(tonic, 0);
    }
}
