package com.music.service;

import com.music.domain.Cadence;
import com.music.domain.Note;

import javax.sound.midi.*;

/**
 * Plays a Cadence via javax.sound.midi on the given Synthesizer.
 * Runs in its own thread; handles interrupts to stop playback cleanly.
 * Supports Note names with or without octave: e.g. "C4" or "D".
 */
public class JavaxMidiPlayer {

    /**
     * Play the given cadence, program and tempo (BPM).
     * If the play thread is interrupted, playback stops.
     */
    public static void play(Cadence cadence, Synthesizer synth, int program, int bpm) {
        try {
            MidiChannel channel = synth.getChannels()[0];
            channel.programChange(program);

            long pause = 60000L / bpm;

            for (Note[] chord : cadence.matrix()) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                // Note‐on for all notes in chord
                for (Note note : chord) {
                    int midiNote = noteToMidi(note);
                    channel.noteOn(midiNote, 80);
                }

                // Pause (interruptible)
                try {
                    Thread.sleep(pause);
                } catch (InterruptedException ie) {
                    // Stop current chord immediately
                    for (Note note : chord) {
                        channel.noteOff(noteToMidi(note));
                    }
                    break;
                }

                // Note‐off for all notes
                for (Note note : chord) {
                    channel.noteOff(noteToMidi(note));
                }
            }
        } catch (InvalidMidiDataException e) {
            // Handles unknown note names
            e.printStackTrace();
        }
    }

    /**
     * Convert a Note (e.g. "C4", "G#3", or just "D") to a MIDI note number.
     * Notes without explicit octave default to octave 4.
     */
    private static int noteToMidi(Note note) throws InvalidMidiDataException {
        String name = note.toString().trim();
        String pitch;
        int octave;

        // If last char is digit, split pitch vs octave
        char last = name.charAt(name.length() - 1);
        if (Character.isDigit(last)) {
            pitch = name.substring(0, name.length() - 1);
            octave = Character.getNumericValue(last);
        } else {
            // no octave in name: default to octave 4
            pitch = name;
            octave = 4;
        }

        int base;
        switch (pitch) {
            case "C":  base = 0;  break;
            case "C#": base = 1;  break;
            case "D":  base = 2;  break;
            case "D#": base = 3;  break;
            case "E":  base = 4;  break;
            case "F":  base = 5;  break;
            case "F#": base = 6;  break;
            case "G":  base = 7;  break;
            case "G#": base = 8;  break;
            case "A":  base = 9;  break;
            case "A#": base = 10; break;
            case "B":  base = 11; break;
            default:
                throw new InvalidMidiDataException("Unknown note name: " + name);
        }
        // MIDI numbering: C-1 = 0, so C0=12, C4=60 etc.
        return (octave + 1) * 12 + base;
    }
}
