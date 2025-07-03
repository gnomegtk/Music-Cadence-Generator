package com.music.service;

import com.music.domain.Cadence;

/**
 * Renders a Cadence as both HTML and MusicXML using only its
 * raw semitone interval data and tonic MIDI base.
 */
public class ScoreRenderer {

    /**
     * Renders a simple HTML preview of the cadence's interval grid and description.
     */
    public static String render(Cadence c) {
        StringBuilder sb = new StringBuilder(
            "<html><body style=\"font-family:monospace;\">");
        sb.append("<h3>").append(c.type()).append("</h3><pre>\n");

        int[][] iv = c.intervals();
        for (int[] row : iv) {
            for (int x : row) {
                sb.append(String.format("%3d", x));
            }
            sb.append("\n");
        }

        sb.append("</pre><p>")
          .append(c.description())
          .append("</p></body></html>");

        return sb.toString();
    }

    /**
     * Exports the Cadence as a single-measure MusicXML file
     * using 3/4 time, treble clef, and 1 chord per quarter.
     *
     * @param cadence The cadence to export
     * @param bpm     Beats per minute (controls tempo marking)
     * @return A MusicXML string
     */
    public static String toMusicXML(Cadence cadence, int bpm) {
        int[][] chords = cadence.intervals();
        int tonicMidi = cadence.getTonicMidi();
        int keyFifths = cadence.getKeySignatureFifths();
        int beats = chords.length;

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<score-partwise version=\"3.1\">\n")
           .append("  <part-list>\n")
           .append("    <score-part id=\"P1\"><part-name>Piano</part-name></score-part>\n")
           .append("  </part-list>\n")
           .append("  <part id=\"P1\">\n")
           .append("    <measure number=\"1\">\n")
           .append("      <attributes>\n")
           .append("        <divisions>1</divisions>\n")
           .append("        <key><fifths>").append(keyFifths).append("</fifths></key>\n")
           .append("        <time><beats>").append(beats).append("</beats><beat-type>4</beat-type></time>\n")
           .append("        <clef><sign>G</sign><line>2</line></clef>\n")
           .append("      </attributes>\n")
           .append("      <direction placement=\"above\">\n")
           .append("        <direction-type><metronome>\n")
           .append("          <beat-unit>quarter</beat-unit>\n")
           .append("          <per-minute>").append(bpm).append("</per-minute>\n")
           .append("        </metronome></direction-type>\n")
           .append("        <sound tempo=\"").append(bpm).append("\"/>\n")
           .append("      </direction>\n");

        for (int[] chord : chords) {
            for (int i = 0; i < chord.length; i++) {
                int midi = tonicMidi + chord[i];
                Pitch p = midiToPitch(midi);

                xml.append("      <note>\n");
                if (i > 0) xml.append("        <chord/>\n");
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
     * Converts a MIDI number to MusicXML pitch components.
     */
    private static Pitch midiToPitch(int midi) {
        int pc  = midi % 12;
        int oct = midi / 12 - 1;
        switch (pc) {
            case 0:  return new Pitch("C",  0, oct);
            case 1:  return new Pitch("C",  1, oct);
            case 2:  return new Pitch("D",  0, oct);
            case 3:  return new Pitch("D",  1, oct);
            case 4:  return new Pitch("E",  0, oct);
            case 5:  return new Pitch("F",  0, oct);
            case 6:  return new Pitch("F",  1, oct);
            case 7:  return new Pitch("G",  0, oct);
            case 8:  return new Pitch("G",  1, oct);
            case 9:  return new Pitch("A",  0, oct);
            case 10: return new Pitch("A",  1, oct);
            case 11: return new Pitch("B",  0, oct);
            default: return new Pitch("C",  0, oct);
        }
    }

    /** Simple record to hold pitch step, accidental, and octave */
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
