package eu.amadeos.games.artillery.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Ranking;
import eu.amadeos.games.artillery.repository.ArtillerySettings;
import eu.amadeos.games.artillery.ui.game.Arena;
import eu.amadeos.games.artillery.ui.game.ArtilleryGame;
import eu.amadeos.games.artillery.ui.main.ArtilleryPresenter;
import eu.amadeos.games.artillery.ui.main.ArtilleryView;
import eu.amadeos.games.artillery.ui.main.ArtilleryWindow;

import java.util.Locale;

/**
 * amade-artillery-game bindings. Theme, language and the XDG directories come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class ArtilleryModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.games.artillery.messages";

    @Override
    protected void configure() {
        bind(ArtillerySettings.class).in(Scopes.SINGLETON);
        bind(Keyboard.class).in(Scopes.SINGLETON);
        bind(Mouse.class).in(Scopes.SINGLETON);
        bind(Mixer.class).in(Scopes.SINGLETON);
        bind(ArtilleryGame.class).in(Scopes.SINGLETON);
        bind(Arena.class).to(ArtilleryGame.class);
        bind(ArtilleryWindow.class).in(Scopes.SINGLETON);
        bind(ArtilleryView.class).to(ArtilleryWindow.class);
        bind(ArtilleryPresenter.class).in(Scopes.SINGLETON);
    }

    /** The best matches in {@code ~/.local/share/amadeos/games/artillery}. */
    @Provides
    @Singleton
    HighScores highScores(XdgDirectories xdg, AppInfo app) {
        return HighScores.of(xdg, app.configName(), Ranking.HIGHER_WINS);
    }

    @Provides
    @Singleton
    PlayerName playerName(AppDirectories directories) {
        return new PlayerName(directories.config(), System.getProperty("user.name"));
    }

    /** The gamepads of this computer, by the backend of its operating system. */
    @Provides
    @Singleton
    Gamepads gamepads() {
        return Gamepads.open();
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(ArtilleryModule.class, MESSAGES, locale);
    }
}
