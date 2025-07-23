package com.music.service;

import com.music.domain.Cadence;
import java.util.Arrays;

/**
 * Renders a Cadence of ABSOLUTE MIDI pitches into MusicXML
 * with two staves: staff 1 (G-clef) for alto & soprano,
 * staff 2 (F-clef) for tenor & bass.
 */
public class ScoreRenderer {

    /**
     * Converts a cadence with 4-voice chords into MusicXML.
     *
     * @param midiCad The cadence containing absolute MIDI pitches.
     * @param bpm     Beats per minute (tempo).
     * @return        A MusicXML document as a string.
     */
    public static String toMusicXMLFromMidi(Cadence midiCad, int bpm) {
        int[][] grid   = midiCad.intervals();
        int     chords = grid.length;

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
            .append("        <staves>2</staves>\n")
            .append("        <key><fifths>0</fifths></key>\n")
            .append("        <time><beats>").append(chords)
            .append("</beats><beat-type>4</beat-type></time>\n")
            .append("        <clef number=\"1\"><sign>G</sign><line>2</line></clef>\n")
            .append("        <clef number=\"2\"><sign>F</sign><line>4</line></clef>\n")
            .append("      </attributes>\n")
            .append("      <direction>\n")
            .append("        <direction-type><metronome>\n")
            .append("          <beat-unit>quarter</beat-unit>\n")
            .append("          <per-minute>").append(bpm).append("</per-minute>\n")
            .append("        </metronome></direction-type>\n")
            .append("      </direction>\n");

        // Staff 1: Alto then Soprano
        for (int i = 0; i < chords; i++) {
            int[] chord = Arrays.copyOf(grid[i], 4);
            Arrays.sort(chord);
            int alto    = chord[2];
            int soprano = chord[3];

            xml.append(renderNote(alto,    1, 1, false, "up"));
            xml.append(renderNote(soprano, 1, 1, true,  "up"));
        }

        // Backup to beginning for staff 2
        xml.append("      <backup><duration>")
           .append(chords).append("</duration></backup>\n");

        // Staff 2: Bass then Tenor
        for (int i = 0; i < chords; i++) {
            int[] chord = Arrays.copyOf(grid[i], 4);
            Arrays.sort(chord);
            int bass  = chord[0];
            int tenor = chord[1];

            xml.append(renderNote(bass,  2, 2, false, "down"));
            xml.append(renderNote(tenor, 2, 2, true,  "down"));
        }

        xml.append("    </measure>\n")
           .append("  </part>\n")
           .append("</score-partwise>\n");
        return xml.toString();
    }

    /**
     * Renders a single MusicXML <note> element.
     */
    private static String renderNote(int midiPitch, int staff, int voice, boolean chord, String stemDir) {
        PitchInfo p = midiToPitch(midiPitch);
        StringBuilder sb = new StringBuilder();
        sb.append("      <note>\n");
        if (chord) sb.append("        <chord/>\n");
        sb.append("        <pitch>\n")
          .append("          <step>").append(p.step).append("</step>\n");
        if (p.alter != 0)
          sb.append("          <alter>").append(p.alter).append("</alter>\n");
        sb.append("          <octave>").append(p.octave).append("</octave>\n")
          .append("        </pitch>\n")
          .append("        <duration>1</duration>\n")
          .append("        <voice>").append(voice).append("</voice>\n")
          .append("        <type>quarter</type>\n")
          .append("        <stem>").append(stemDir).append("</stem>\n")
          .append("        <staff>").append(staff).append("</staff>\n")
          .append("      </note>\n");
        return sb.toString();
    }

    private static class PitchInfo {
        final String step;
        final int alter;
        final int octave;
        PitchInfo(String step, int alter, int octave) {
            this.step   = step;
            this.alter  = alter;
            this.octave = octave;
        }
    }

    /**
     * Converts a MIDI number into step/alter/octave.
     */
    private static PitchInfo midiToPitch(int midi) {
        int octave = (midi / 12) - 1;
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

        return new PitchInfo(step, alter, octave);
    }
}
