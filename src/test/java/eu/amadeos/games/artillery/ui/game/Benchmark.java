package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.games.artillery.bootstrap.ArtilleryModule;
import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Field;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Theme;
import eu.amadeos.games.artillery.repository.ArtillerySettings;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * How long a frame of each landscape takes to draw, at two window sizes: {@code ./gradlew benchmark}. Four
 * computers fight, so shells fly, the ground breaks and settles and the smoke rises. The frames are drawn
 * into a compatible image, as the canvas draws, but without waiting for the screen.
 */
public final class Benchmark {
    static final int WARM_UP = 60;
    static final int FRAMES = 300;

    public static void main(String[] args) throws Exception {
        Messages msg = Messages.load(ArtilleryModule.class, ArtilleryModule.MESSAGES, Locale.ENGLISH);
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        ArtilleryGame game = new ArtilleryGame(new Keyboard(), new Mouse(), Gamepads.none(), msg,
                new ArtillerySettings(Files.createTempDirectory("artillery").resolve("artillery.properties")), mixer,
                Runnable::run);
        GraphicsConfiguration gc = GraphicsEnvironment.isHeadless() ? null
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        List<Seat> seats = List.of(new Seat(0, Control.HARD), new Seat(1, Control.NORMAL), new Seat(2, Control.EASY),
                new Seat(3, Control.HARD));
        System.out.println("ms per frame");
        for (int scale : new int[]{2, 3}) {
            for (Theme theme : Theme.values()) {
                game.play(new Match.Setup(seats, 1, theme), theme.ordinal());
                int w = (int) Field.WIDTH * scale;
                int h = (int) Field.HEIGHT * scale;
                BufferedImage image = gc != null ? gc.createCompatibleImage(w, h) : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                long total = 0;
                for (int i = 0; i < WARM_UP + FRAMES; i++) {
                    game.update(ArtilleryGame.STEP);
                    game.update(ArtilleryGame.STEP);
                    Graphics2D g = image.createGraphics();
                    g.scale(scale, scale);
                    long start = System.nanoTime();
                    game.render(g, 0.5);
                    game.renderOverlay(g, 0.5);
                    if (i >= WARM_UP) total += System.nanoTime() - start;
                    g.dispose();
                }
                System.out.printf(Locale.ROOT, "%-6s at %4d x %4d   %6.2f%n", theme.id(), w, h, total / 1e6 / FRAMES);
            }
        }
        System.exit(0);
    }
}
