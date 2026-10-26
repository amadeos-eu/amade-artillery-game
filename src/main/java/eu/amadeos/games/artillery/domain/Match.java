package eu.amadeos.games.artillery.domain;

import eu.amadeos.gameengine.random.SeededRandom;

import java.util.ArrayList;
import java.util.List;

/**
 * A match of rounds: who plays, how many rounds a seat must win, the landscape (or a different one each
 * round), the rounds won and the points. Each round is a new {@link Battle}; the seat who begins moves on
 * by one each round.
 */
public final class Match {
    /** The rules chosen before the match. {@code theme} {@code null}: a different landscape each round. */
    public record Setup(List<Seat> seats, int toWin, Theme theme) {
        public Setup {
            if (seats.size() < 2 || seats.size() > Seat.MAX) throw new IllegalArgumentException("2 to 4 players");
            if (toWin < 1) throw new IllegalArgumentException("at least one round to win");
            seats = List.copyOf(seats);
        }

        /** A person against the computer, first to two rounds, a different landscape each round. */
        public static Setup standard() {
            return new Setup(List.of(new Seat(0, Control.PERSON), new Seat(1, Control.NORMAL)), 2, null);
        }
    }

    /** How a match ended: the seat with most rounds won ({@code null} if two share the most), its points, and whether a person won. */
    public record Result(Seat winner, int points, int rounds) {
        public boolean personWon() {
            return winner != null && !winner.control().computer();
        }
    }

    private final Setup setup;
    private final SeededRandom random;
    private final int[] wins;
    private final int[] points;
    private final int firstTheme;
    private int round;
    private Battle battle;

    public Match(Setup setup, SeededRandom random) {
        this.setup = setup;
        this.random = random;
        this.wins = new int[Seat.MAX];
        this.points = new int[Seat.MAX];
        this.firstTheme = random.nextInt(Theme.values().length);
    }

    // --8<-- [start:round]
    /** The next round: its landscape, and the next seat begins. */
    public Battle nextRound() {
        Theme theme = setup.theme() != null ? setup.theme()
                : Theme.values()[(firstTheme + round) % Theme.values().length];
        battle = Battle.create(theme, setup.seats(), round % setup.seats().size(), random.fork());
        round++;
        return battle;
    }

    /** Counts the round that has just ended: the winner's round, everybody's points. */
    public void roundOver() {
        for (Tank t : battle.tanks()) points[t.seat().number()] += t.damageDealt() + t.kills() * Field.KILL_POINTS;
        Tank winner = battle.winner();
        if (winner != null) {
            wins[winner.seat().number()]++;
            points[winner.seat().number()] += Field.ROUND_POINTS;
        }
    }

    /** A seat has won enough rounds. */
    public boolean over() {
        for (Seat s : setup.seats()) if (wins[s.number()] >= setup.toWin()) return true;
        return false;
    }
    // --8<-- [end:round]

    public Result result() {
        Seat best = null;
        boolean shared = false;
        for (Seat s : setup.seats()) {
            if (best == null || wins[s.number()] > wins[best.number()]) {
                best = s;
                shared = false;
            } else if (wins[s.number()] == wins[best.number()]) {
                shared = true;
            }
        }
        return shared ? new Result(null, 0, round) : new Result(best, points[best.number()], round);
    }

    public Setup setup() {
        return setup;
    }

    public Battle battle() {
        return battle;
    }

    /** The number of the current round, from 1. */
    public int round() {
        return round;
    }

    public int wins(Seat seat) {
        return wins[seat.number()];
    }

    public int points(Seat seat) {
        return points[seat.number()];
    }

    /** The seats in the order of the table: most rounds won first, then most points. */
    public List<Seat> standings() {
        List<Seat> list = new ArrayList<>(setup.seats());
        list.sort((a, b) -> wins[a.number()] != wins[b.number()] ? wins[b.number()] - wins[a.number()]
                : points[b.number()] - points[a.number()]);
        return list;
    }
}
