package com.music.service;

import com.music.domain.Cadence;
import java.util.Arrays;

/**
 * Renders a Cadence of ABSOLUTE MIDI pitches directly to MusicXML.
 * Logs the grid on entry so you can verify exactly what is being rendered.
 */
public class ScoreRenderer {

    /**
     * Build a simple MusicXML string for the given cadence of absolute MIDI pitches.
     *
     * @param midiCad the cadence whose intervals() are real MIDI numbers
     * @param bpm     metronome marking (beats per minute)
     * @return        a MusicXML document representing those exact MIDI pitches
     */
    public static String toMusicXMLFromMidi(Cadence midiCad, int bpm) {
        // 1) pull out the 2D array of MIDI pitches
        int[][] grid = midiCad.intervals();

        // 2) debug‐print it so you see exactly what’s going out
        System.out.println(">>> in toMusicXMLFromMidi: " 
            + Arrays.deepToString(grid));

        // 3) now render to MusicXML without any re‐spelling logic
        StringBuilder xml = new StringBuilder()
            .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<score-partwise version=\"3.1\">\n")
            .append("  <part-list>\n")
            .append("    <score-part id=\"P1\"><part-name>Piano</part-name></score-part>\n")
            .append("  </part-list>\n")
            .append("  <part id=\"P1\">\n")
            .append("    <measure number=\"1\">\n")
            .append("      <attributes>\n")
            .append("        <divisions>1</divisions>\n")
            .append("        <time><beats>").append(grid.length)
            .append("</beats><beat-type>4</beat-type></time>\n")
            .append("        <clef><sign>G</sign><line>2</line></clef>\n")
            .append("      </attributes>\n")
            .append("      <direction>\n")
            .append("        <direction-type><metronome>\n")
            .append("          <beat-unit>quarter</beat-unit>\n")
            .append("          <per-minute>").append(bpm).append("</per-minute>\n")
            .append("        </metronome></direction-type>\n")
            .append("      </direction>\n");

        for (int i = 0; i < grid.length; i++) {
            xml.append("      <!-- chord ")
               .append(Arrays.toString(grid[i]))
               .append(" -->\n");
            for (int j = 0; j < grid[i].length; j++) {
                int midi = grid[i][j];
                Pitch p  = midiToPitch(midi);
                xml.append("      <note>\n");
                if (j > 0) xml.append("        <chord/>\n");
                xml.append("        <pitch>\n")
                   .append("          <step>").append(p.step).append("</step>\n");
                if (p.alter != 0) {
                    xml.append("          <alter>").append(p.alter).append("</alter>\n");
                }
                xml.append("          <octave>").append(p.octave).append("</octave>\n")
                   .append("        </pitch>\n")
                   .append("        <duration>1</duration>\n")
                   .append("        <type>quarter</type>\n")
                   .append("      </note>\n");
            }
        }

        xml.append("    </measure>\n")
           .append("  </part>\n")
           .append("</score-partwise>\n");

        return xml.toString();
    }

    /**
     * Convert a MIDI number into MusicXML pitch data.
     */
    private static Pitch midiToPitch(int midi) {
        int octave = (midi / 12) - 1; // MIDI 0 = C–1
        int pc     = midi % 12;
        String step;
        int alter;

        switch (pc) {
            case 0:  step = "C"; alter = 0; break;
            case 1:  step = "C"; alter = 1; break;
            case 2:  step = "D"; alter = 0; break;
            case 3:  step = "D"; alter = 1; break;
            case 4:  step = "E"; alter = 0; break;
            case 5:  step = "F"; alter = 0; break;
            case 6:  step = "F"; alter = 1; break;
            case 7:  step = "G"; alter = 0; break;
            case 8:  step = "G"; alter = 1; break;
            case 9:  step = "A"; alter = 0; break;
            case 10: step = "A"; alter = 1; break;
            case 11: step = "B"; alter = 0; break;
            default: throw new IllegalArgumentException("Invalid MIDI note: " + midi);
        }

        return new Pitch(step, alter, octave);
    }

    private static class Pitch {
        final String step;
        final int alter;
        final int octave;
        Pitch(String step, int alter, int octave) {
            this.step   = step;
            this.alter  = alter;
            this.octave = octave;
        }
    }
}
