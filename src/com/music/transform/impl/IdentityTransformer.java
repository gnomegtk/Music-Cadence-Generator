package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

/**
 * No-op: returns its input unchanged.
 */
public class IdentityTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        return c;
    }
}
