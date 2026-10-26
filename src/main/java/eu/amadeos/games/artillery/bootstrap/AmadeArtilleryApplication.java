package eu.amadeos.games.artillery.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.games.artillery.ui.game.ArtilleryGame;
import eu.amadeos.games.artillery.ui.main.ArtilleryPresenter;
import eu.amadeos.games.artillery.ui.main.ArtilleryWindow;

public final class AmadeArtilleryApplication {
    public static final AppInfo APP = new AppInfo("amade-artillery-game", "Artillery", "artillery");

    private AmadeArtilleryApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeArtilleryApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            Mixer mixer = injector.getInstance(Mixer.class);
            mixer.start();
            Gamepads gamepads = injector.getInstance(Gamepads.class);
            ArtilleryWindow window = injector.getInstance(ArtilleryWindow.class);
            ArtilleryPresenter presenter = injector.getInstance(ArtilleryPresenter.class);
            ArtilleryGame game = injector.getInstance(ArtilleryGame.class);
            window.bind(presenter);
            presenter.start();
            window.whenClosed(() -> {
                game.stop();
                gamepads.close();
                mixer.close();
                System.exit(0);
            });
            window.setLocationByPlatform(true);
            window.setVisible(true);
            game.start(window.canvas());
        }, new ArtilleryModule());
    }
    // --8<-- [end:main]
}
