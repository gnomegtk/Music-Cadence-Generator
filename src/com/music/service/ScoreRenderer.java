package com.music.service;

import com.music.domain.Cadence;
import com.music.domain.Note;
import com.music.util.KeySignatureHelper;

import java.util.Arrays;

/**
 * Renders a Cadence to MusicXML.
 * All HTML/preview logic has been removed;
 * this class now has a single responsibility.
 */
public class ScoreRenderer {

    /**
     * Build a simple MusicXML for the given cadence at the specified tempo.
     *
     * @param c     the cadence to export
     * @param tonic the key in which to spell notes (e.g. "C", "Eb", "F#")
     * @param bpm   the metronome marking in beats per minute
     * @return a MusicXML string representing the cadence
     */
    public static String toMusicXML(Cadence c, String tonic, int bpm) {
        int[][] midiGrid    = c.intervals();
        Note[][] matrix     = KeySignatureHelper.computeMatrix(midiGrid, tonic);
        int     fifths      = KeySignatureHelper.getKeySignatureFifths(tonic);

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
            .append("        <key><fifths>").append(fifths).append("</fifths></key>\n")
            .append("        <time><beats>").append(matrix.length)
            .append("</beats><beat-type>4</beat-type></time>\n")
            .append("        <clef><sign>G</sign><line>2</line></clef>\n")
            .append("      </attributes>\n")
            .append("      <direction>\n")
            .append("        <direction-type><metronome>\n")
            .append("          <beat-unit>quarter</beat-unit>\n")
            .append("          <per-minute>").append(bpm).append("</per-minute>\n")
            .append("        </metronome></direction-type>\n")
            .append("      </direction>\n");

        for (int i = 0; i < matrix.length; i++) {
            xml.append("      <!-- chord ")
               .append(Arrays.toString(midiGrid[i]))
               .append(" -->\n");
            for (int j = 0; j < matrix[i].length; j++) {
                Note n = matrix[i][j];
                xml.append("      <note>\n");
                if (j > 0) {
                    xml.append("        <chord/>\n");
                }
                xml.append("        <pitch>\n")
                   .append("          <step>").append(n.step()).append("</step>\n");
                if (n.alter() != 0) {
                    xml.append("          <alter>").append(n.alter()).append("</alter>\n");
                }
                xml.append("          <octave>").append(n.octave()).append("</octave>\n")
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
}
