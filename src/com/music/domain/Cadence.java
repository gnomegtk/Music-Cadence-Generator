package com.music.domain;

import java.util.List;
import java.util.Objects;
import java.util.Arrays;

public class Cadence {
    private final List<Note> notes;
    private final String label;
    private final int[][] intervals;
    private final Note[][] matrix;
    private final String type;
    private final String description;

    public Cadence(List<Note> notes, String label) {
        this.notes = notes;
        this.label = label;
        this.intervals = null;
        this.matrix = null;
        this.type = null;
        this.description = null;
    }

    // novo construtor com 4 parâmetros para registry
    public Cadence(String type, int[][] intervals, Note[][] matrix, String description) {
        this.notes = null;
        this.label = null;
        this.type = type;
        this.intervals = intervals;
        this.matrix = matrix;
        this.description = description;
    }

    public List<Note> notes() {
        return notes;
    }

    public String label() {
        return label;
    }

    public int[][] intervals() {
        return intervals;
    }

    public Note[][] matrix() {
        return matrix;
    }

    public String type() {
        return type;
    }

    public String description() {
        return description;
    }

    @Override
    public String toString() {
        return label != null ? label : type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cadence)) return false;
        Cadence cadence = (Cadence) o;
        return Objects.equals(notes, cadence.notes) &&
               Objects.equals(label, cadence.label) &&
               Objects.deepEquals(matrix, cadence.matrix) &&
               Objects.equals(type, cadence.type) &&
               Objects.equals(description, cadence.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notes, label, type, description, Arrays.deepHashCode(matrix));
    }
}
