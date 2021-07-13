package com.bcom.nsplacer.misc;

import lombok.Getter;
import lombok.Setter;

import java.util.Random;

@Getter
@Setter
public class InitializerParameters {

    private Random random;
    private int randRange, randOffset;
    private int fixed;

    public InitializerParameters(Random random, int randRange, int randOffset, int fixed) {
        this.random = random;
        this.randRange = randRange;
        this.randOffset = randOffset;
        this.fixed = fixed;
    }

    public int get() {
        if (random != null) {
            return Math.abs(random.nextInt()) % randRange + randOffset;
        } else {
            return fixed;
        }
    }
}
