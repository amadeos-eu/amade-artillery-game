package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.games.artillery.domain.Match;

import java.util.function.Consumer;

/**
 * What the presenter may ask of the running game. The game runs on its own thread; every method here is
 * safe to call from the Swing thread and takes effect at the start of the game's next step.
 */
public interface Arena {
    /** Back to the title screen, where the computers fight. */
    void showTitle();

    /** The card where the players, the rounds and the landscape are chosen. */
    void newGame();

    /** Pauses a running battle; does nothing elsewhere. */
    void pause();

    void setSound(boolean on);

    /** Which gamepads the game sees, for the About dialog: "Gamepads: XInput, 1 connected". */
    String gamepads();

    /** Called on the Swing thread when a match ends. */
    void onMatchOver(Consumer<Match.Result> listener);
}
