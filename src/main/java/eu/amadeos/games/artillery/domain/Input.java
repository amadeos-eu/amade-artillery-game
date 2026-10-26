package eu.amadeos.games.artillery.domain;

/**
 * What the player of the tank whose turn it is does in a step. {@code turn} and {@code power} are held
 * keys, -1, 0 or 1 (turn: 1 turns the barrel to the left, towards 180 degrees; power: 1 is more);
 * {@code weapon} picks the next (1) or the previous (-1) one; {@code fire} fires. The mouse and the
 * computer set the aim directly: {@code angle} and {@code setPower} are then numbers, otherwise NaN.
 */
public record Input(int turn, int power, int weapon, boolean fire, double angle, double setPower) {
    public static final Input NONE = new Input(0, 0, 0, false, Double.NaN, Double.NaN);

    public static Input keys(int turn, int power, int weapon, boolean fire) {
        return new Input(turn, power, weapon, fire, Double.NaN, Double.NaN);
    }

    public static Input aim(double angle, double power, boolean fire) {
        return new Input(0, 0, 0, fire, angle, power);
    }

    public static Input firing() {
        return keys(0, 0, 0, true);
    }
}
