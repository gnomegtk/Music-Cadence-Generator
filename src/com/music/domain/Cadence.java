package com.music.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * A harmonic cadence including a type label, description,
 * tonic (key), and an interval grid (and optionally a note matrix).
 */
public class Cadence {
    private final String type;
    private final String description;
    private final String tonic;
    private final int[][] intervals;
    private final Note[][] matrix;

    /** Primary constructor */
    public Cadence(String type,
                   String description,
                   String tonic,
                   int[][] intervals,
                   Note[][] matrix) {
        this.type        = type;
        this.description = description;
        this.tonic       = tonic;
        this.intervals   = intervals;
        this.matrix      = matrix;
    }

    /** Legacy constructor to support transformer return values */
    public Cadence(String type,
                   int[][] intervals,
                   Note[][] matrix,
                   String description) {
        this(type, description, "C", intervals, matrix);
    }

    public String type()        { return type; }
    public String description() { return description; }
    public String tonic()       { return tonic; }
    public int[][] intervals()  { return intervals; }
    public Note[][] matrix()    { return matrix; }

    /**
     * Gets the tonic MIDI number at octave 4.
     * For example: C4 = 60, D♭4 = 61, E4 = 64, etc.
     */
    public int getTonicMidi() {
        Map<String, Integer> map = new HashMap<>();
        map.put("C",  0); map.put("C#", 1); map.put("Db", 1);
        map.put("D",  2); map.put("D#", 3); map.put("Eb", 3);
        map.put("E",  4); map.put("Fb", 4); map.put("E#", 5);
        map.put("F",  5); map.put("F#", 6); map.put("Gb", 6);
        map.put("G",  7); map.put("G#", 8); map.put("Ab", 8);
        map.put("A",  9); map.put("A#",10); map.put("Bb",10);
        map.put("B", 11); map.put("Cb",11); map.put("B#", 0);

        int offset = map.getOrDefault(tonic, 0);
        return 60 + offset;
    }

    /**
     * Returns key signature fifths for MusicXML.
     * C=0, G=+1, D=+2 ... F=-1, Bb=-2, Eb=-3 ...
     */
    public int getKeySignatureFifths() {
        switch (tonic) {
            case "G":  return  1;
            case "D":  return  2;
            case "A":  return  3;
            case "E":  return  4;
            case "B":  return  5;
            case "F#": return  6;
            case "C#": return  7;
            case "F":  return -1;
            case "Bb": return -2;
            case "Eb": return -3;
            case "Ab": return -4;
            case "Db": return -5;
            case "Gb": return -6;
            case "Cb": return -7;
            default:   return  0;
        }
    }
}
