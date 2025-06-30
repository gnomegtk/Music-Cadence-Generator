package com.music.service;

import com.music.domain.Cadence;
import com.music.domain.Note;

/**
 * Renders HTML and MusicXML output from a Cadence using interval-based voicing.
 */
public class ScoreRenderer {

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

    public static String toMusicXML(Cadence cadence) {
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

        int[][] intervals = cadence.intervals();
        for (int[] chord : intervals) {
            int[] pitches = applyVoicingFromIntervals(chord, 60); // C4 = MIDI 60

            for (int i = 0; i < pitches.length; i++) {
                Pitch p = midiToPitch(pitches[i]);

                xml.append("      <note>\n");
                if (i > 0) xml.append("        <chord/>\n");
                xml.append("        <pitch>\n");
                xml.append("          <step>").append(p.step).append("</step>\n");
                if (p.alter != 0)
                    xml.append("          <alter>").append(p.alter).append("</alter>\n");
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

    private static int[] applyVoicingFromIntervals(int[] chord, int basePitch) {
        int[] result = new int[chord.length];
        for (int i = 0; i < chord.length; i++) {
            result[i] = basePitch + chord[i];
        }
        return result;
    }

    private static Pitch midiToPitch(int midi) {
        int note = midi % 12;
        int octave = (midi / 12) - 1;

        if (note == 0) return new Pitch("C", 0, octave);
        if (note == 1) return new Pitch("C", 1, octave);
        if (note == 2) return new Pitch("D", 0, octave);
        if (note == 3) return new Pitch("D", 1, octave);
        if (note == 4) return new Pitch("E", 0, octave);
        if (note == 5) return new Pitch("F", 0, octave);
        if (note == 6) return new Pitch("F", 1, octave);
        if (note == 7) return new Pitch("G", 0, octave);
        if (note == 8) return new Pitch("G", 1, octave);
        if (note == 9) return new Pitch("A", 0, octave);
        if (note == 10) return new Pitch("A", 1, octave);
        if (note == 11) return new Pitch("B", 0, octave);

        return new Pitch("C", 0, octave);
    }

    private static class Pitch {
        String step;
        int alter;
        int octave;

        Pitch(String step, int alter, int octave) {
            this.step = step;
            this.alter = alter;
            this.octave = octave;
        }
    }
}
