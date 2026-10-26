package eu.amadeos.games.artillery.domain;

/** The materials of the ground, as the {@code Ground} of the engine keeps them; the user interface colours them by theme. */
public final class Earth {
    /** The body of the earth. */
    public static final byte SOIL = 1;
    /** The top layer: grass, sand, snow or dust. */
    public static final byte TOP = 2;
    /** Earth thrown by a dirt shot. */
    public static final byte DIRT = 3;
    /** The burnt rim of a crater. */
    public static final byte SCORCHED = 4;
    /** How deep the top layer reaches, in units. */
    public static final double TOP_DEPTH = 2.5;

    private Earth() {
    }
}
