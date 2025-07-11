package com.music.domain;

import com.music.domain.Note;

/**
 * A chord progression defined purely by its semitone intervals,
 * plus an optional legacy spelled-note matrix and a description.
 * Carries NO key information (no tonic, no MIDI base, no fifths).
 */
public class Cadence {

    private final String   type;
    private final int[][]  intervals;
    private final Note[][] matrix;      // may be null
    private final String   description;

    public Cadence(String type,
                   int[][] intervals,
                   Note[][] matrix,
                   String description) {
        this.type        = type;
        this.intervals   = intervals;
        this.matrix      = matrix;
        this.description = description;
    }

    public String   type()        { return type;        }
    public int[][]  intervals()   { return intervals;   }
    public Note[][] matrix()      { return matrix;      }
    public String   description() { return description; }
}
