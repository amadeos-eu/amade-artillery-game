package eu.amadeos.games.artillery.domain;

/** What a tank can fire, how big the blast is, how much it hurts and how many a tank has in a round. */
public enum Weapon {
    // --8<-- [start:weapons]
    /** The plain shell: as many as wanted. */
    SHELL(14, 35, 1, -1),
    /** Twice the blast, a deep crater. */
    HEAVY(26, 60, 1, 2),
    /** Three small shells, fanned out: hard to miss with, each hurts less. */
    TRIPLE(11, 24, 3, 2),
    /** Earth instead of a blast: builds a wall, or buries a tank. */
    DIRT(22, 0, 1, 2);
    // --8<-- [end:weapons]

    /** The angle between the shells of a {@link #TRIPLE}, in degrees. */
    public static final double SPREAD = 4;

    private final double radius;
    private final int damage;
    private final int shells;
    private final int stock;

    Weapon(double radius, int damage, int shells, int stock) {
        this.radius = radius;
        this.damage = damage;
        this.shells = shells;
        this.stock = stock;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The radius of the blast, or of the heap of earth. */
    public double radius() {
        return radius;
    }

    /** The damage in the middle of the blast. */
    public int damage() {
        return damage;
    }

    /** How many shells one shot fires. */
    public int shells() {
        return shells;
    }

    /** How many a tank has at the start of a round; -1 for no limit. */
    public int stock() {
        return stock;
    }

    public boolean heapsEarth() {
        return this == DIRT;
    }
}
