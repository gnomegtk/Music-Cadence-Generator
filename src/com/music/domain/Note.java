package com.music.domain;

/**
 * Represents a single pitched note: step (A–G), accidental (alter), and octave.
 * 
 * Two constructors:
 *   1) Note(step, alter, octave) – full control.
 *   2) Note(name)             – legacy, parses "C", "C#", "Db", defaults to octave 4.
 */
public class Note {
    private final String step;   // "A" through "G"
    private final int    alter;  // -1 = flat, 0 = natural, +1 = sharp
    private final int    octave; // MIDI octave number (e.g. 4 for middle C)

    /**
     * Primary constructor.
     *
     * @param step   note letter "A"–"G"
     * @param alter  -1=b, 0=natural, +1=#
     * @param octave octave number (e.g. 4 → C4 = MIDI 60)
     */
    public Note(String step, int alter, int octave) {
        this.step   = step;
        this.alter  = alter;
        this.octave = octave;
    }

    /**
     * Legacy one-arg constructor for backwards compatibility.
     * Parses names like "C", "C#", "Db" and sets octave to 4.
     *
     * @param name pitch name with optional accidental (e.g. "Eb")
     */
    public Note(String name) {
        // Extract letter and accidental
        String letter = name.substring(0, 1);
        int alt = 0;
        if (name.length() > 1) {
            char acc = name.charAt(1);
            if (acc == '#')        alt = 1;
            else if (acc == 'b' 
                     || acc == 'B') alt = -1;
        }
        // Delegate to main constructor, default octave = 4
        this.step   = letter;
        this.alter  = alt;
        this.octave = 4;
    }

    public String step()   { return step; }
    public int    alter()  { return alter; }
    public int    octave() { return octave; }

    @Override
    public String toString() {
        String acc = alter ==  1 ? "#" 
                   : alter == -1 ? "b" 
                   : "";
        return step + acc + octave;
    }

    /**
     * Converts this Note into its absolute MIDI note number.
     * Follows: C4 = 60, C#4 = 61, …, B4 = 71, etc.
     */
    public int toMidi() {
        int base;
        switch (step) {
            case "C": base = 0;  break;
            case "D": base = 2;  break;
            case "E": base = 4;  break;
            case "F": base = 5;  break;
            case "G": base = 7;  break;
            case "A": base = 9;  break;
            case "B": base = 11; break;
            default:  base = 0;  break;
        }
        // (octave+1)*12 aligns so that C4 = 60
        return (octave + 1) * 12 + base + alter;
    }
}
