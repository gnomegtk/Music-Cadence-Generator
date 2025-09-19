package com.music.util;

import com.music.domain.Note;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Arrays;

/**
 * Spells absolute MIDI pitches into Note(step, alter, octave),
 * applying key‐specific enharmonic overrides for both sharps and flats,
 * and supplies MusicXML <fifths> values for the key signature.
 */
public class KeySignatureHelper {

    // Default PC→label
    private static final String[] PC_LABELS = {
        "C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"
    };

    // tonic → (pc → override label)
    private static final Map<String, Map<Integer,String>> OVERRIDES = new LinkedHashMap<>();
    private static final Map<String,Integer> FIFTHS = new LinkedHashMap<>();
    private static final Map<String,String> MINOR_TO_MAJOR = new LinkedHashMap<>();

    static {
        OVERRIDES.put("C",  map(3,"E♭", 8,"A♭", 10,"B♭"));               // C major
        OVERRIDES.put("G",  map(3,"E♭", 10,"B♭"));                       // G major
        OVERRIDES.put("D",  map(1,"C♯", 3,"E♭", 10,"B♭"));               // D major
        OVERRIDES.put("A",  map(1,"C♯", 3,"E♭", 6,"F♯"));                // A major
        OVERRIDES.put("E",  map(1,"C♯", 4,"E♯", 6,"F♯"));                // E major
        OVERRIDES.put("B",  map(1,"C♯", 4,"E♯", 6,"F♯"));                // B major
        OVERRIDES.put("F#", map(1,"C♯", 4,"E♯", 6,"F♯"));                // F♯ major
        OVERRIDES.put("C#", map(0,"B♯", 5,"E♯", 11,"B♯"));               // C♯ major

        OVERRIDES.put("F",  map(8,"A♭", 11,"B♭"));                       // F major
        OVERRIDES.put("Bb", map(3,"E♭", 10,"B♭"));                       // B♭ major
        OVERRIDES.put("Eb", map(3,"E♭", 8,"A♭", 10,"B♭"));               // E♭ major
        OVERRIDES.put("Ab", map(1,"D♭", 3,"E♭", 8,"A♭", 10,"B♭"));       // A♭ major

	OVERRIDES.put("Am",  map(3,"E♭", 8,"A♭", 10,"B♭"));              // A minor
        OVERRIDES.put("Em",  map(3,"E♭", 10,"B♭"));                      // E minor
        OVERRIDES.put("Bm",  map(1,"C♯", 3,"E♭", 10,"B♭"));              // B minor
        OVERRIDES.put("F#m", map(1,"C♯", 4,"E♯", 6,"F♯"));              // F♯ minor
        OVERRIDES.put("C#m", map(0,"B♯", 5,"E♯", 11,"B♯"));             // C♯ minor
        OVERRIDES.put("G#m", map(1,"C♯", 4,"E♯", 6,"F♯"));              // G♯ minor
        OVERRIDES.put("D#m", map(1,"C♯", 4,"E♯", 6,"F♯"));              // D♯ minor
        OVERRIDES.put("A#m", map(1,"C♯", 4,"E♯", 6,"F♯"));              // A♯ minor

        OVERRIDES.put("Dm",  map(3,"E♭", 8,"A♭", 10,"B♭"));              // D minor
        OVERRIDES.put("Gm",  map(3,"E♭", 10,"B♭"));                      // G minor
        OVERRIDES.put("Cm",  map(3,"E♭", 8,"A♭", 10,"B♭"));              // C minor
        OVERRIDES.put("Fm",  map(1,"D♭", 3,"E♭", 8,"A♭", 10,"B♭"));      // F minor

        // MusicXML <fifths>
        FIFTHS.put("C", 0);  FIFTHS.put("G", 1);  FIFTHS.put("D", 2);
        FIFTHS.put("A", 3);  FIFTHS.put("E", 4);  FIFTHS.put("B", 5);
        FIFTHS.put("F#",6);  FIFTHS.put("C#",7);
        FIFTHS.put("F",-1);  FIFTHS.put("Bb",-2); FIFTHS.put("Eb",-3);
        FIFTHS.put("Ab",-4); FIFTHS.put("Db",-5); FIFTHS.put("Gb",-6);
        FIFTHS.put("Cb",-7);

        // Minor → relative major
        MINOR_TO_MAJOR.put("Am","C"); MINOR_TO_MAJOR.put("Em","G");
        MINOR_TO_MAJOR.put("Bm","D"); MINOR_TO_MAJOR.put("F#m","A");
        MINOR_TO_MAJOR.put("C#m","E");MINOR_TO_MAJOR.put("G#m","B");
        MINOR_TO_MAJOR.put("D#m","F#");MINOR_TO_MAJOR.put("A#m","C#");
        MINOR_TO_MAJOR.put("Dm","F"); MINOR_TO_MAJOR.put("Gm","Bb");
        MINOR_TO_MAJOR.put("Cm","Eb");MINOR_TO_MAJOR.put("Fm","Ab");
        MINOR_TO_MAJOR.put("Bbm","Db");MINOR_TO_MAJOR.put("Ebm","Gb");
        MINOR_TO_MAJOR.put("Abm","Cb");
    }

    private static Map<Integer,String> map(Object... kv) {
        Map<Integer,String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((Integer)kv[i], (String)kv[i+1]);
        }
        return m;
    }

    private static String resolveKey(String tonic) {
        return MINOR_TO_MAJOR.getOrDefault(tonic, tonic);
    }

    /**
     * Spell one absolute MIDI pitch into a Note.
     * Applies PC_LABELS plus any key-specific override,
     * and corrects octave for B♯.
     */
    public static Note midiToNote(int midi, String tonic) {
        String key = resolveKey(tonic);
        int pc = (midi % 12 + 12) % 12;
        int octave = midi / 12 - 1;

        Map<Integer,String> ov = OVERRIDES.get(key);
        String label = (ov != null && ov.containsKey(pc))
                     ? ov.get(pc)
                     : PC_LABELS[pc];

        // octave fix: B♯ is really a C one semitone up
        if ("B♯".equals(label)) {
            octave--;
        }

        String step = label.substring(0,1);
        int alter = label.contains("♯") ? 1 : label.contains("♭") ? -1 : 0;

        return new Note(step, alter, octave);
    }

    public static Note[][] computeMatrix(int[][] grid, String tonic) {
        Note[][] out = new Note[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            out[i] = new Note[grid[i].length];
            for (int j = 0; j < grid[i].length; j++) {
                out[i][j] = midiToNote(grid[i][j], tonic);
            }
        }
        return out;
    }

    /** MusicXML fifths */
    public static int getKeySignatureFifths(String tonic) {
        String key = resolveKey(tonic);
        return FIFTHS.getOrDefault(key, 0);
    }
}
