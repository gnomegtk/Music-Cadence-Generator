package com.music.service;

import com.music.domain.Cadence;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;

/**
 * Plays raw interval data from Cadence.intervals(),
 * simply adding each semitone offset to the tonic MIDI pitch.
 */
public class JavaxMidiPlayer {

    /**
     * @param cadence  already-transposed cadence
     * @param synth    open Java MIDI synthesizer
     * @param bank     MIDI bank number
     * @param program  MIDI program number
     * @param bpm      tempo in quarter-notes per minute
     */
    public static void play(Cadence cadence,
                            Synthesizer synth,
                            int bank,
                            int program,
                            int bpm) {
        try {
            MidiChannel channel = synth.getChannels()[0];
            channel.programChange(bank, program);

            int tonicMidi = cadence.getTonicMidi();
            int quarterMs = 60000 / bpm;
            int[][] chords = cadence.intervals();

            for (int[] chord : chords) {
                // note-on for every semitone offset
                for (int semitone : chord) {
                    channel.noteOn(tonicMidi + semitone, 100);
                }
                Thread.sleep(quarterMs);
                // note-off
                for (int semitone : chord) {
                    channel.noteOff(tonicMidi + semitone, 100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
