package com.music.service;

import com.music.domain.Cadence;
import com.music.domain.Note;

import javax.sound.midi.*;

/**
 * Plays a Cadence using javax.sound.midi with register-preserving voicing.
 */
public class JavaxMidiPlayer {

    /**
     * Plays the cadence using the given Synthesizer, applying MIDI program and tempo.
     * Notes are voiced using calculated intervals (not hardcoded octave tiers).
     */
    public static void play(Cadence cadence, Synthesizer synth, int program, int bpm) {
        try {
            MidiChannel channel = synth.getChannels()[0];
            channel.programChange(program);

            long pause = 60000L / bpm;

            int[][] intervals = cadence.intervals();
            for (int[] chord : intervals) {
                if (Thread.currentThread().isInterrupted()) break;

                int[] pitches = applyVoicingFromIntervals(chord, 60); // C4 as base

                for (int midi : pitches) {
                    channel.noteOn(midi, 90);
                }

                try {
                    Thread.sleep(pause);
                } catch (InterruptedException ex) {
                    for (int midi : pitches) {
                        channel.noteOff(midi);
                    }
                    break;
                }

                for (int midi : pitches) {
                    channel.noteOff(midi);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts intervals to MIDI pitches starting from a base note.
     */
    private static int[] applyVoicingFromIntervals(int[] chord, int basePitch) {
        int[] result = new int[chord.length];
        for (int i = 0; i < chord.length; i++) {
            result[i] = basePitch + chord[i];
        }
        return result;
    }
}
