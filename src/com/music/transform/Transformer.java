package com.music.transform;

import com.music.domain.Cadence;

/**
 * A matrix-based cadence transformer.
 */
public interface Transformer {
    /**
     * Takes a Cadence’s raw intervals + tonic → returns a new Cadence.
     */
    Cadence transform(Cadence cadence, String tonic);
}
