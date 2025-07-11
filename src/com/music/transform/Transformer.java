package com.music.transform;

import com.music.domain.Cadence;

/**
 * Generic cadence‐transformer interface.
 */
public interface Transformer {
    Cadence transform(Cadence input);
}
