package eu.amadeos.games.artillery.domain;

import eu.amadeos.gameengine.random.SeededRandom;

import java.util.List;
import java.util.function.Predicate;

/** Helpers for the tests: battles with computers at the controls, run until something is true. */
final class Battles {
    static final double STEP = Battle.step();

    private Battles() {
    }

    static List<Seat> seats(Control... controls) {
        Seat[] seats = new Seat[controls.length];
        for (int i = 0; i < controls.length; i++) seats[i] = new Seat(i, controls[i]);
        return List.of(seats);
    }

    static Battle battle(Theme theme, long seed, Control... controls) {
        return Battle.create(theme, seats(controls), 0, new SeededRandom(seed));
    }

    /** Runs the battle with computers for every tank until the condition holds or the seconds are up; returns the seconds taken. */
    static double runUntil(Battle battle, Computer computer, double maxSeconds, Predicate<Battle> done) {
        double t = 0;
        while (t < maxSeconds && !done.test(battle)) {
            battle.update(STEP, computer.steer(battle, STEP));
            t += STEP;
        }
        return t;
    }

    /** Steps without input. */
    static void idle(Battle battle, double seconds) {
        for (int i = 0; i < Math.round(seconds / STEP); i++) battle.update(STEP, Input.NONE);
    }
}
