package eu.amadeos.games.artillery.domain;

/** A place in a match: its number (0 to 3, also the colour: red, blue, green, gold) and who plays it. */
public record Seat(int number, Control control) {
    public static final int MAX = 4;

    public Seat {
        if (number < 0 || number >= MAX) throw new IllegalArgumentException("seat " + number);
    }
}
