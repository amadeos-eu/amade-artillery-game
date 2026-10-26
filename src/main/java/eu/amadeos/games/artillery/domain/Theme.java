package eu.amadeos.games.artillery.domain;

/**
 * The landscapes: how high and how rough the ground is, how strong gravity, whether there is wind. Their
 * pictures are drawn by the user interface.
 */
public enum Theme {
    // --8<-- [start:themes]
    /** An alpine valley at sunset: mountains and a deep valley. */
    VALLEY(290, 170, 0.55, 1, true),
    /** Dunes under a white sun: low, smooth and long. */
    DESERT(300, 110, 0.42, 1, true),
    /** A winter night: high, jagged peaks. */
    WINTER(275, 210, 0.62, 1, true),
    /** The moon: weak gravity, no air and no wind, old craters. */
    MOON(300, 120, 0.5, 0.4, false);
    // --8<-- [end:themes]

    private final double base;
    private final double amplitude;
    private final double roughness;
    private final double gravity;
    private final boolean windy;

    Theme(double base, double amplitude, double roughness, double gravity, boolean windy) {
        this.base = base;
        this.amplitude = amplitude;
        this.roughness = roughness;
        this.gravity = gravity;
        this.windy = windy;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public double base() {
        return base;
    }

    public double amplitude() {
        return amplitude;
    }

    public double roughness() {
        return roughness;
    }

    /** Gravity as a part of the green world's. */
    public double gravity() {
        return gravity;
    }

    public boolean windy() {
        return windy;
    }
}
