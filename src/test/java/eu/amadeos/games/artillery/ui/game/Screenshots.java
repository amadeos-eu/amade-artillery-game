package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.games.artillery.bootstrap.ArtilleryModule;
import eu.amadeos.games.artillery.domain.Battle;
import eu.amadeos.games.artillery.domain.Computer;
import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Theme;
import eu.amadeos.games.artillery.repository.ArtillerySettings;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * The screenshots of chapter 29 and of the README, drawn without a screen: the title, the setup card, a
 * shell in flight over each landscape, earth falling into a crater, the replay and the end of a round.
 * {@code ./gradlew screenshots -Pshots=<folder>}.
 */
public final class Screenshots {
    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final ArtilleryGame game;

    private Screenshots() throws Exception {
        Messages msg = Messages.load(ArtilleryModule.class, ArtilleryModule.MESSAGES, Locale.ENGLISH);
        ArtillerySettings settings = new ArtillerySettings(Files.createTempDirectory("artillery").resolve("artillery.properties"));
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        game = new ArtilleryGame(keyboard, new Mouse(), Gamepads.none(), msg, settings, mixer, Runnable::run);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "screenshots");
        dir.mkdirs();
        Screenshots shots = new Screenshots();
        shots.steps(6 * 120);
        shots.save(new File(dir, "title.png"));
        shots.tap(KeyEvent.VK_ENTER);
        shots.steps(30);
        shots.save(new File(dir, "setup.png"));

        // Each landscape: a match of three, until a shell of the second turn is high in the air.
        for (Theme theme : Theme.values()) {
            ArtilleryGame.PlayScene play = shots.game.play(new Match.Setup(List.of(new Seat(0, Control.PERSON),
                    new Seat(1, Control.NORMAL), new Seat(2, Control.HARD)), 2, theme), 40 + theme.ordinal());
            shots.computers(play, b -> b.turnNumber() == 1 && b.phase() == Battle.Phase.FLIGHT
                    && !b.shells().isEmpty() && b.shells().getFirst().projectile().vy > -20);
            shots.save(new File(dir, theme.id() + ".png"));
        }
        shots.save(new File(dir, "../screenshot.png"));

        // A heavy shell under a tank: the crater, and the earth and the tank falling into it.
        ArtilleryGame.PlayScene crater = shots.game.play(new Match.Setup(List.of(new Seat(0, Control.HARD),
                new Seat(1, Control.HARD)), 2, Theme.VALLEY), 7);
        shots.computers(crater, b -> b.blasts().stream().anyMatch(x -> x.progress() > 0.3));
        shots.save(new File(dir, "crater.png"));

        // A whole round, then the replay and the card.
        ArtilleryGame.PlayScene round = shots.game.play(new Match.Setup(List.of(new Seat(0, Control.HARD),
                new Seat(1, Control.NORMAL), new Seat(2, Control.EASY)), 2, Theme.WINTER), 12);
        shots.computers(round, b -> b.phase() == Battle.Phase.OVER);
        shots.seconds(ArtilleryGame.OVER_SECONDS + 0.1);
        if (shots.game.top() instanceof ArtilleryGame.ReplayScene replay) {
            for (int i = 0; i < 120 * 20 && !(replay.battle().blasts().stream().anyMatch(x -> x.progress() > 0.25)); i++) shots.steps(1);
            shots.save(new File(dir, "replay.png"));
            shots.tap(KeyEvent.VK_ENTER);
        }
        shots.seconds(1);
        shots.save(new File(dir, "round.png"));
    }

    private void computers(ArtilleryGame.PlayScene play, Predicate<Battle> until) {
        Computer computer = new Computer(new SeededRandom(1));
        Battle battle = play.battle();
        for (int i = 0; i < 120 * 600 && !until.test(battle); i++) {
            now[0] += (long) (ArtilleryGame.STEP * 1e9);
            play.step(ArtilleryGame.STEP, battle.current().seat().control().computer() ? computer.steer(battle, ArtilleryGame.STEP)
                    : computer.steer(battle, ArtilleryGame.STEP));
            if (battle.phase() == Battle.Phase.OVER && game.top() == play) {
                game.update(ArtilleryGame.STEP);
                if (until.test(battle)) break;
            }
        }
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(1);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (ArtilleryGame.STEP * 1e9);
            game.update(ArtilleryGame.STEP);
        }
    }

    private void seconds(double s) {
        steps((int) Math.round(s * ArtilleryGame.UPDATES_PER_SECOND));
    }

    private void save(File file) throws Exception {
        BufferedImage image = new BufferedImage(1280, 800, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(2, 2);
        game.render(g, 1);
        game.renderOverlay(g, 1);
        g.dispose();
        ImageIO.write(image, "png", file);
    }
}
