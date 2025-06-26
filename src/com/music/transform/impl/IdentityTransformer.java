package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * Identity – no change to the progression.
 * E.g. [2,5,9] → [2,5,9]
 */
public class IdentityTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c, String t) {
        return new Cadence(
            "Identity of " + c.type(),
            c.intervals(),
            c.matrix(),
            "Identity – no intervals changed, e.g. [2,5,9] → [2,5,9]"
        );
    }
}
