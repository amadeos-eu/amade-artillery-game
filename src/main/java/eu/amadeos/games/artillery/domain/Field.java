package eu.amadeos.games.artillery.domain;

/**
 * The numbers of the battlefield in one place: its size, gravity and wind, the tanks, how fast the barrel
 * turns, how much a fall hurts. Units are the game's logical units; the screen is 640 × 400 of them.
 */
public final class Field {
    public static final double WIDTH = 640;
    public static final double HEIGHT = 400;
    /** The top bar. */
    public static final double TOP = 24;
    /** Two cells of earth per unit: craters with round edges, and little to compute. */
    public static final double CELL = 0.5;
    /** How many cells the loose earth falls in a step: a unit, 120 units a second. */
    public static final int SETTLE_PASSES = 2;

    // --8<-- [start:physics]
    /** Gravity on the green world, in units per second per second. */
    public static final double GRAVITY = 120;
    /** The speed of a shell fired at full power: at 45 degrees it would fly about 900 units, over the field and more. */
    public static final double MAX_SPEED = 330;
    /** The strongest wind, shown as 10; each point pushes a shell with this many units per second per second. */
    public static final int MAX_WIND = 10;
    public static final double WIND_PUSH = 5;
    /** How much the wind may change between two turns, at most. */
    public static final int WIND_CHANGE = 4;
    // --8<-- [end:physics]

    public static final double TANK_WIDTH = 22;
    public static final double TANK_HEIGHT = 12;
    /** The turret's pivot above the tracks, and the length of the barrel. */
    public static final double PIVOT = 9;
    public static final double BARREL = 15;
    public static final int HEALTH = 100;

    /** Degrees and points of power per second while a key is held, after the first step of a tap. */
    public static final double TURN_SPEED = 40;
    public static final double POWER_SPEED = 30;
    /** How long a key must be held before the barrel turns on by itself. */
    public static final double HOLD_DELAY = 0.3;

    /** A fall up to this deep does no harm; deeper costs {@link #FALL_DAMAGE} per unit. */
    public static final double SAFE_FALL = 8;
    public static final double FALL_DAMAGE = 1.5;

    /** A destroyed tank blows up after a moment, with a crater and damage of its own. */
    public static final double WRECK_DELAY = 0.35;
    public static final double WRECK_RADIUS = 18;
    public static final int WRECK_DAMAGE = 20;
    public static final double EXPLOSION_SECONDS = 0.45;
    /** The short calm after everything has settled, before the next turn. */
    public static final double CALM_SECONDS = 0.5;
    /** A shell that flies longer than this is gone. */
    public static final double MAX_FLIGHT = 15;

    /** Points: one per point of damage to others, these for a tank destroyed and a round won. */
    public static final int KILL_POINTS = 50;
    public static final int ROUND_POINTS = 100;

    private Field() {
    }
}
