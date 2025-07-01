package com.music.service;

import com.music.domain.Cadence;
import com.music.domain.Note;

/**
 * Renders HTML preview and MusicXML export of a Cadence,
 * using interval-based voicing and including tempo markings.
 */
public class ScoreRenderer {

    /**
     * Creates an HTML snippet showing the interval matrix and description.
     */
    public static String render(Cadence c) {
        StringBuilder sb = new StringBuilder("<html><body style=\"font-family:monospace;\">");
        sb.append("<h3>").append(c.type()).append("</h3><pre>");
        for (int[] row : c.intervals()) {
            for (int cell : row) {
                sb.append(String.format("%3d", cell));
            }
            sb.append("\n");
        }
        sb.append("</pre><p>").append(c.description()).append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Generates MusicXML with consistent voicing and a tempo marking.
     *
     * @param cadence  the Cadence to export
     * @param tempoBPM the tempo in beats per minute
     * @return a MusicXML document as String
     */
    public static String toMusicXML(Cadence cadence, int tempoBPM) {
        StringBuilder xml = new StringBuilder();

        // Header and part-list
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<score-partwise version=\"3.1\">\n");
        xml.append("  <part-list>\n");
        xml.append("    <score-part id=\"P1\">\n");
        xml.append("      <part-name>Cadence</part-name>\n");
        xml.append("    </score-part>\n");
        xml.append("  </part-list>\n");
        xml.append("  <part id=\"P1\">\n");
        xml.append("    <measure number=\"1\">\n");
        xml.append("      <attributes>\n");
        xml.append("        <divisions>1</divisions>\n");
        xml.append("        <key><fifths>0</fifths></key>\n");
        xml.append("        <time><beats>4</beats><beat-type>4</beat-type></time>\n");
        xml.append("        <clef><sign>G</sign><line>2</line></clef>\n");
        xml.append("      </attributes>\n");

        // Tempo marking
        xml.append("      <direction placement=\"above\">\n");
        xml.append("        <direction-type>\n");
        xml.append("          <metronome>\n");
        xml.append("            <beat-unit>quarter</beat-unit>\n");
        xml.append("            <per-minute>").append(tempoBPM).append("</per-minute>\n");
        xml.append("          </metronome>\n");
        xml.append("        </direction-type>\n");
        xml.append("        <sound tempo=\"").append(tempoBPM).append("\"/>\n");
        xml.append("      </direction>\n");

        // Notes as simultaneous quarter-note chords
        for (int[] chord : cadence.intervals()) {
            int[] pitches = applyVoicingFromIntervals(chord, 60); // base = C4

            for (int i = 0; i < pitches.length; i++) {
                Pitch p = midiToPitch(pitches[i]);
                xml.append("      <note>\n");
                if (i > 0) xml.append("        <chord/>\n");
                xml.append("        <pitch>\n");
                xml.append("          <step>").append(p.step).append("</step>\n");
                if (p.alter != 0) {
                    xml.append("          <alter>").append(p.alter).append("</alter>\n");
                }
                xml.append("          <octave>").append(p.octave).append("</octave>\n");
                xml.append("        </pitch>\n");
                xml.append("        <duration>1</duration>\n");
                xml.append("        <type>quarter</type>\n");
                xml.append("      </note>\n");
            }
        }

        // Close measure/part/score
        xml.append("    </measure>\n");
        xml.append("  </part>\n");
        xml.append("</score-partwise>\n");

        return xml.toString();
    }

    /** Adds the chord’s semitone offsets to a base MIDI pitch. */
    private static int[] applyVoicingFromIntervals(int[] chord, int basePitch) {
        int[] result = new int[chord.length];
        for (int i = 0; i < chord.length; i++) {
            result[i] = basePitch + chord[i];
        }
        return result;
    }

    /** Converts a MIDI note number into MusicXML pitch components. */
    private static Pitch midiToPitch(int midi) {
        int pc = midi % 12;
        int octave = (midi / 12) - 1;
        switch (pc) {
            case 0:  return new Pitch("C", 0, octave);
            case 1:  return new Pitch("C", 1, octave);
            case 2:  return new Pitch("D", 0, octave);
            case 3:  return new Pitch("D", 1, octave);
            case 4:  return new Pitch("E", 0, octave);
            case 5:  return new Pitch("F", 0, octave);
            case 6:  return new Pitch("F", 1, octave);
            case 7:  return new Pitch("G", 0, octave);
            case 8:  return new Pitch("G", 1, octave);
            case 9:  return new Pitch("A", 0, octave);
            case 10: return new Pitch("A", 1, octave);
            case 11: return new Pitch("B", 0, octave);
            default: return new Pitch("C", 0, octave);
        }
    }

    /** Simple container for MusicXML pitch fields. */
    private static class Pitch {
        final String step;
        final int alter;
        final int octave;
        Pitch(String step, int alter, int octave) {
            this.step = step;
            this.alter = alter;
            this.octave = octave;
        }
    }
}
