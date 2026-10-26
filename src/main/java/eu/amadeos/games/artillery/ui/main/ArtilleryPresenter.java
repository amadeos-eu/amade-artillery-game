package eu.amadeos.games.artillery.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Score;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.repository.ArtillerySettings;
import eu.amadeos.games.artillery.ui.game.Arena;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Everything around the battle, without Swing: the menu commands, the settings, pausing when the window
 * loses the focus, and the best matches. All methods run on the Swing thread.
 */
public final class ArtilleryPresenter {
    /** The one table: the points of the matches won by a person. */
    static final String TABLE = "matches";

    private final ArtilleryView view;
    private final Arena arena;
    private final Messages msg;
    private final ArtillerySettings settings;
    private final HighScores scores;
    private final Supplier<Instant> now;

    @Inject
    public ArtilleryPresenter(ArtilleryView view, Arena arena, Messages msg, ArtillerySettings settings, HighScores scores) {
        this(view, arena, msg, settings, scores, Instant::now);
    }

    ArtilleryPresenter(ArtilleryView view, Arena arena, Messages msg, ArtillerySettings settings, HighScores scores,
                       Supplier<Instant> now) {
        this.view = view;
        this.arena = arena;
        this.msg = msg;
        this.settings = settings;
        this.scores = scores;
        this.now = now;
    }

    public void start() {
        boolean sound = settings.sound();
        RenderMode mode = settings.renderMode();
        view.showOptions(sound, mode);
        view.useRenderMode(mode);
        arena.setSound(sound);
        arena.onMatchOver(this::matchOver);
        arena.showTitle();
    }

    /** Opens the card where the players and the rounds are chosen. */
    public void newGame() {
        arena.newGame();
    }

    public void pause() {
        arena.pause();
    }

    /** The window lost the focus: nobody is at the keys, so a running battle waits. */
    public void focusLost() {
        arena.pause();
    }

    public void setSound(boolean on) {
        settings.setSound(on);
        arena.setSound(on);
    }

    public void setRenderMode(RenderMode mode) {
        settings.setRenderMode(mode);
        view.useRenderMode(mode);
    }

    public void showScores() {
        view.showScores(-1);
    }

    public void showAbout() {
        view.showAbout(arena.gamepads());
    }

    /** A match ended: one won by a person enters the table with its points. */
    void matchOver(Match.Result result) {
        if (!result.personWon() || result.points() <= 0 || !scores.qualifies(TABLE, result.points())) return;
        view.askName(msg.format("scores.reached", result.points(), result.rounds())).ifPresent(name -> {
            int place = scores.add(TABLE, new Score(name, result.points(), now.get()));
            view.showScores(place);
        });
    }
}
