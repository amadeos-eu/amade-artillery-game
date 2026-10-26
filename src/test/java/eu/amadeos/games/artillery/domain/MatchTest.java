package eu.amadeos.games.artillery.domain;

import eu.amadeos.gameengine.random.SeededRandom;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {
    private static void play(Match match, Computer computer) {
        Battles.runUntil(match.battle(), computer, 900, b -> b.phase() == Battle.Phase.OVER);
        match.roundOver();
    }

    @Test
    void theSeatThatBeginsMovesOnAndTheLandscapeChanges() {
        Match match = new Match(new Match.Setup(Battles.seats(Control.HARD, Control.HARD, Control.HARD), 3, null),
                new SeededRandom(1));
        Battle first = match.nextRound();
        assertEquals(0, first.current().seat().number());
        Battle second = match.nextRound();
        assertEquals(1, second.current().seat().number());
        assertNotEquals(first.theme(), second.theme());
        assertEquals(2, match.round());
    }

    @Test
    void theFirstToWinEnoughRoundsWinsTheMatch() {
        Match match = new Match(new Match.Setup(Battles.seats(Control.HARD, Control.EASY), 2, Theme.DESERT),
                new SeededRandom(5));
        Computer computer = new Computer(new SeededRandom(5));
        while (!match.over()) {
            match.nextRound();
            assertEquals(Theme.DESERT, match.battle().theme());
            play(match, computer);
            assertTrue(match.round() <= 5, "a match of first-to-two takes three rounds at most, unless rounds are drawn");
        }
        Match.Result result = match.result();
        assertEquals(2, match.wins(result.winner()));
        assertEquals(match.standings().getFirst(), result.winner());
        assertTrue(result.points() >= 2 * Field.ROUND_POINTS + Field.KILL_POINTS, "rounds, a kill and damage: " + result.points());
        assertFalse(result.personWon());
    }

    @Test
    void aMatchWithTheMostRoundsSharedHasNoWinner() {
        Match match = new Match(new Match.Setup(Battles.seats(Control.PERSON, Control.NORMAL), 3, null), new SeededRandom(1));
        assertNull(match.result().winner());
    }

    @Test
    void twoToFourPlayers() {
        assertThrows(IllegalArgumentException.class, () -> new Match.Setup(Battles.seats(Control.PERSON), 2, null));
        Match.Setup standard = Match.Setup.standard();
        assertEquals(List.of(Control.PERSON, Control.NORMAL), standard.seats().stream().map(Seat::control).toList());
    }
}
