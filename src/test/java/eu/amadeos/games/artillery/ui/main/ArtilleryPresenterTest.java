package eu.amadeos.games.artillery.ui.main;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.artillery.bootstrap.ArtilleryModule;
import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.repository.ArtillerySettings;
import eu.amadeos.games.artillery.ui.game.Arena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtilleryPresenterTest {
    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();
    private Consumer<Match.Result> matchOver;
    private ArtilleryPresenter presenter;
    private HighScores scores;
    private ArtillerySettings settings;

    private final ArtilleryView view = new ArtilleryView() {
        @Override
        public void showOptions(boolean sound, RenderMode mode) {
            log.add("options " + sound + " " + mode);
        }

        @Override
        public void useRenderMode(RenderMode mode) {
            log.add("mode " + mode);
        }

        @Override
        public Optional<String> askName(String message) {
            log.add("ask " + message);
            return Optional.of("Ada");
        }

        @Override
        public void showScores(int highlight) {
            log.add("scores " + highlight);
        }

        @Override
        public void showAbout(String gamepads) {
            log.add("about " + gamepads);
        }
    };

    private final Arena arena = new Arena() {
        @Override
        public void showTitle() {
            log.add("title");
        }

        @Override
        public void newGame() {
            log.add("new");
        }

        @Override
        public void pause() {
            log.add("pause");
        }

        @Override
        public void setSound(boolean on) {
            log.add("sound " + on);
        }

        @Override
        public String gamepads() {
            return "Gamepads: none, 0 connected";
        }

        @Override
        public void onMatchOver(Consumer<Match.Result> listener) {
            matchOver = listener;
        }
    };

    @BeforeEach
    void create() {
        Messages msg = Messages.load(ArtilleryModule.class, ArtilleryModule.MESSAGES, Locale.ENGLISH);
        settings = new ArtillerySettings(dir.resolve("artillery.properties"));
        scores = new HighScores(dir.resolve("scores"), Ranking.HIGHER_WINS);
        presenter = new ArtilleryPresenter(view, arena, msg, settings, scores, () -> Instant.EPOCH);
        presenter.start();
    }

    @Test
    void theSettingsAreAppliedAtStartAndKept() {
        assertEquals(List.of("options true DIRECT", "mode DIRECT", "sound true", "title"), log);
        log.clear();
        presenter.setRenderMode(RenderMode.PIXEL);
        presenter.setSound(false);
        assertEquals(List.of("mode PIXEL", "sound false"), log);
        assertEquals(RenderMode.PIXEL, settings.renderMode());
    }

    @Test
    void aMatchWonByAPersonEntersTheTable() {
        log.clear();
        matchOver.accept(new Match.Result(new Seat(0, Control.PERSON), 340, 3));
        assertEquals(List.of("ask You won the match: 340 points in 3 rounds.", "scores 0"), log);
        assertEquals("Ada", scores.list(ArtilleryPresenter.TABLE).getFirst().name());
        assertEquals(340, scores.list(ArtilleryPresenter.TABLE).getFirst().value());
    }

    @Test
    void aMatchWonByTheComputerOrDrawnEntersNothing() {
        log.clear();
        matchOver.accept(new Match.Result(new Seat(1, Control.HARD), 400, 2));
        matchOver.accept(new Match.Result(null, 120, 2));
        assertEquals(List.of(), log);
    }

    @Test
    void losingTheFocusPausesTheShift() {
        log.clear();
        presenter.focusLost();
        presenter.newGame();
        presenter.showAbout();
        assertEquals(List.of("pause", "new", "about Gamepads: none, 0 connected"), log);
    }
}
