package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * The card before a match, without drawing: how many play, who drives each tank, how many rounds win the
 * match, and the landscape. Up and down choose a row, left and right change it, Enter on the last row
 * starts.
 */
final class SetupCard {
    enum Row { PLAYERS, SEAT, ROUNDS, LANDSCAPE, START }

    /** A row of the card; {@code seat} is the seat's number for {@link Row#SEAT}, else -1. */
    record Line(Row row, int seat) {
    }

    private int players;
    private final Control[] controls = new Control[Seat.MAX];
    private int toWin;
    /** 0: a different landscape each round; 1 to 4: one of the themes. */
    private int landscape;
    private int selected;

    SetupCard(Match.Setup setup) {
        players = setup.seats().size();
        for (int i = 0; i < Seat.MAX; i++) controls[i] = i == 0 ? Control.PERSON : Control.NORMAL;
        for (Seat s : setup.seats()) controls[s.number()] = s.control();
        toWin = setup.toWin();
        landscape = setup.theme() == null ? 0 : setup.theme().ordinal() + 1;
        selected = lines().size() - 1;
    }

    List<Line> lines() {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Row.PLAYERS, -1));
        for (int i = 0; i < players; i++) lines.add(new Line(Row.SEAT, i));
        lines.add(new Line(Row.ROUNDS, -1));
        lines.add(new Line(Row.LANDSCAPE, -1));
        lines.add(new Line(Row.START, -1));
        return lines;
    }

    int selected() {
        return selected;
    }

    void select(int index) {
        selected = Math.max(0, Math.min(lines().size() - 1, index));
    }

    void move(int direction) {
        select(Math.floorMod(selected + direction, lines().size()));
    }

    /** Changes the selected row one step: to the right {@code 1}, to the left {@code -1}. */
    void change(int direction) {
        Line line = lines().get(selected);
        switch (line.row()) {
            case PLAYERS -> {
                players = Math.max(2, Math.min(Seat.MAX, players + direction));
                selected = 0;
            }
            case SEAT -> controls[line.seat()] = Control.values()[Math.floorMod(controls[line.seat()].ordinal() + direction,
                    Control.values().length)];
            case ROUNDS -> toWin = Math.max(1, Math.min(5, toWin + direction));
            case LANDSCAPE -> landscape = Math.floorMod(landscape + direction, Theme.values().length + 1);
            case START -> {
            }
        }
    }

    int players() {
        return players;
    }

    Control control(int seat) {
        return controls[seat];
    }

    int toWin() {
        return toWin;
    }

    /** The landscape chosen, or {@code null} for a different one each round. */
    Theme theme() {
        return landscape == 0 ? null : Theme.values()[landscape - 1];
    }

    Match.Setup setup() {
        List<Seat> seats = new ArrayList<>();
        for (int i = 0; i < players; i++) seats.add(new Seat(i, controls[i]));
        return new Match.Setup(seats, toWin, theme());
    }
}
