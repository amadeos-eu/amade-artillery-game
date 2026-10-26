package eu.amadeos.games.artillery.ui.main;

import eu.amadeos.gameengine.swing.RenderMode;

import java.util.Optional;

/** What {@link ArtilleryPresenter} needs from the window; tests replace it. The battle itself is drawn by the game. */
public interface ArtilleryView {
    /** Shows the settings in the menu. */
    void showOptions(boolean sound, RenderMode mode);

    /** Switches the canvas to a render mode. */
    void useRenderMode(RenderMode mode);

    /** Asks for the player's name after a won match; empty if cancelled. */
    Optional<String> askName(String message);

    /** Shows the best matches; {@code highlight} -1 for no new entry. */
    void showScores(int highlight);

    /** The About dialog, with a line on the gamepads. */
    void showAbout(String gamepads);
}
