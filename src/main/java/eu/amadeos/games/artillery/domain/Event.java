package eu.amadeos.games.artillery.domain;

/** What happened in a step of a battle; the user interface turns it into pictures and sounds. */
public sealed interface Event {
    /** It is a tank's turn; the wind for it. */
    record TurnStarted(Tank tank, int wind) implements Event {
    }

    /** A tank fired, from the end of its barrel. */
    record Fired(Tank tank, Weapon weapon, double x, double y) implements Event {
    }

    /** A blast and its crater; {@code weapon} is {@code null} when a destroyed tank blew up. */
    record Exploded(double x, double y, double radius, Weapon weapon) implements Event {
    }

    /** A dirt shot landed and heaped up earth. */
    record Heaped(double x, double y, double radius) implements Event {
    }

    /** A tank was hurt. */
    record Hit(Tank tank, int damage) implements Event {
    }

    /** A tank's health is gone; it blows up a moment later. */
    record Destroyed(Tank tank) implements Event {
    }

    /** A tank fell when the ground under it went, and took damage if it was deep. */
    record Fell(Tank tank, double depth, int damage) implements Event {
    }

    /** One tank is left, or none ({@code winner} is then {@code null}). */
    record RoundOver(Tank winner) implements Event {
    }
}
