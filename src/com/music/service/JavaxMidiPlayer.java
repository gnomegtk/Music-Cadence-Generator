package com.music.service;

import com.music.domain.Cadence;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;

/**
 * Plays a Cadence of absolute MIDI numbers—no octave math here.
 */
public class JavaxMidiPlayer {

    public static void play(Cadence c,
                            Synthesizer synth,
                            int bank,
                            int program,
                            int bpm) throws InterruptedException {
        MidiChannel ch = synth.getChannels()[0];
        ch.programChange(bank, program);
        int ms = 60000 / bpm;

        for (int[] chord : c.intervals()) {
            for (int m : chord)        ch.noteOn(m, 100);
            Thread.sleep(ms);
            for (int m : chord)        ch.noteOff(m);
        }
    }
}
