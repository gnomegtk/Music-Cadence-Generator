package com.music.service;

import com.music.domain.Cadence;
import com.music.domain.Note;

import java.util.List;

/**
 * Renders a Cadence to HTML (render) and MusicXML (toMusicXML).
 * The MusicXML output groups each chord’s notes under the same measure,
 * using <chord/> for all but the first note in each chord, and sets
 * each chord to a quarter-note duration.
 */
public class ScoreRenderer {

    /**
     * Return an HTML snippet of the matrix for embedding in Swing.
     */
    public static String render(Cadence c) {
        StringBuilder sb = new StringBuilder("<html><body style=\"font-family:monospace;\">");
        sb.append("<h3>").append(c.type()).append("</h3><pre>");
        int[][] iv = c.intervals();
        for (int[] row : iv) {
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
     * Produce full MusicXML document for this cadence, with each chord
     * as simultaneous quarter notes.
     */
    public static String toMusicXML(Cadence c) {
        StringBuilder xml = new StringBuilder();
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

        // Each chord as a group of <note>, all with duration 1 (quarter)
        Note[][] matrix = c.matrix();
        for (Note[] chord : matrix) {
            // first note: no <chord/>
            for (int i = 0; i < chord.length; i++) {
                Note note = chord[i];
                xml.append("      <note>\n");
                if (i > 0) {
                    xml.append("        <chord/>\n");
                }
                // pitch
                String name = note.toString();
                Pitch p = parsePitch(name);
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

        xml.append("    </measure>\n");
        xml.append("  </part>\n");
        xml.append("</score-partwise>\n");
        return xml.toString();
    }

    /**
     * Parse a Note name like "C4", "G#3" or "D" into MusicXML pitch components.
     * Notes without an octave default to octave 4.
     */
    private static Pitch parsePitch(String name) {
        String pitch;
        int octave;
        name = name.trim();
        char last = name.charAt(name.length() - 1);
        if (Character.isDigit(last)) {
            pitch = name.substring(0, name.length() - 1);
            octave = Character.getNumericValue(last);
        } else {
            pitch = name;
            octave = 4;
        }

        int alter = 0;
        String step = pitch;
        if (pitch.endsWith("#")) {
            step = pitch.substring(0, pitch.length() - 1);
            alter = 1;
        }

        return new Pitch(step, alter, octave);
    }

    /** Helper struct for MusicXML pitch. */
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
