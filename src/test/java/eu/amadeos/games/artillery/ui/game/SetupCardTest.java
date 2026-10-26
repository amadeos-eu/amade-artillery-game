package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Theme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetupCardTest {
    @Test
    void theCardStartsOnStartWithTheGivenSetup() {
        SetupCard card = new SetupCard(Match.Setup.standard());
        assertEquals(SetupCard.Row.START, card.lines().get(card.selected()).row());
        assertEquals(Match.Setup.standard(), card.setup());
    }

    @Test
    void morePlayersAddSeatRowsAndKeepTheirDrivers() {
        SetupCard card = new SetupCard(Match.Setup.standard());
        card.select(0);
        card.change(1);
        card.change(1);
        card.change(1);
        assertEquals(4, card.players());
        assertEquals(8, card.lines().size());
        card.select(4);
        card.change(1);
        assertEquals(Control.HARD, card.control(3));
        card.select(0);
        card.change(-1);
        card.change(-1);
        card.change(-1);
        assertEquals(2, card.players());
        card.change(1);
        card.change(1);
        assertEquals(Control.HARD, card.control(3), "a seat taken away and given back keeps its driver");
    }

    @Test
    void driversRoundsAndLandscapeGoRound() {
        SetupCard card = new SetupCard(new Match.Setup(List.of(new Seat(0, Control.PERSON), new Seat(1, Control.HARD)), 5, Theme.MOON));
        card.select(2);
        card.change(1);
        assertEquals(Control.PERSON, card.control(1));
        card.select(3);
        card.change(1);
        assertEquals(5, card.toWin(), "five rounds at most");
        card.select(4);
        card.change(1);
        assertNull(card.theme(), "after the moon comes a changing landscape");
        card.change(1);
        assertEquals(Theme.VALLEY, card.theme());
    }

    @Test
    void upAndDownGoRoundTheCard() {
        SetupCard card = new SetupCard(Match.Setup.standard());
        card.move(1);
        assertEquals(0, card.selected());
        card.move(-1);
        assertEquals(card.lines().size() - 1, card.selected());
    }
}
