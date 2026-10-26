package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.games.artillery.bootstrap.ArtilleryModule;
import eu.amadeos.games.artillery.domain.Battle;
import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Field;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Tank;
import eu.amadeos.games.artillery.domain.Theme;
import eu.amadeos.games.artillery.domain.Weapon;
import eu.amadeos.games.artillery.repository.ArtillerySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The game without a window and without the loop thread: the test presses keys, moves the mouse and calls
 * the steps itself, so everything happens in order on the test thread.
 */
class ArtilleryGameTest {
    private static final double STEP = ArtilleryGame.STEP;

    @TempDir
    Path dir;

    private final long[] now = {0};
    private final Keyboard keyboard = new Keyboard(() -> now[0]);
    private final Mouse mouse = new Mouse();
    private final List<Match.Result> results = new ArrayList<>();
    private ArtillerySettings settings;
    private ArtilleryGame game;

    @BeforeEach
    void create() {
        Messages msg = Messages.load(ArtilleryModule.class, ArtilleryModule.MESSAGES, Locale.ENGLISH);
        Mixer mixer = new Mixer();
        mixer.setMuted(true);
        settings = new ArtillerySettings(dir.resolve("artillery.properties"));
        game = new ArtilleryGame(keyboard, mouse, Gamepads.none(), msg, settings, mixer, Runnable::run);
        game.onMatchOver(results::add);
    }

    private void tap(int key) {
        keyboard.pressed(key, now[0]);
        keyboard.released(key, now[0] + 1_000_000);
        steps(4);                                 // a release counts once it is surely not a key repeat
    }

    private void click(double x, double y) {
        mouse.moved(x, y);
        mouse.pressed(MouseEvent.BUTTON1);
        mouse.released(MouseEvent.BUTTON1);
        steps(1);
    }

    private void steps(int n) {
        for (int i = 0; i < n; i++) {
            now[0] += (long) (STEP * 1e9);
            game.update(STEP);
        }
    }

    private void seconds(double seconds) {
        steps((int) Math.round(seconds * ArtilleryGame.UPDATES_PER_SECOND));
    }

    private ArtilleryGame.SetupScene setup() {
        tap(KeyEvent.VK_ENTER);
        return assertInstanceOf(ArtilleryGame.SetupScene.class, game.top());
    }

    private ArtilleryGame.PlayScene personAgainstComputer() {
        return game.play(new Match.Setup(List.of(new Seat(0, Control.PERSON), new Seat(1, Control.EASY)), 2, Theme.VALLEY), 7);
    }

    @Test
    void enterOpensTheCardAndEnterThereStartsTheMatch() {
        assertInstanceOf(ArtilleryGame.TitleScene.class, game.top());
        setup();
        tap(KeyEvent.VK_ENTER);
        ArtilleryGame.PlayScene play = assertInstanceOf(ArtilleryGame.PlayScene.class, game.top());
        assertEquals(Match.Setup.standard(), play.match().setup());
        assertEquals(1, play.match().round());
    }

    @Test
    void theCardIsChangedWithTheArrowsAndKeptForTheNextMatch() {
        ArtilleryGame.SetupScene setup = setup();
        tap(KeyEvent.VK_UP);                      // from Start up to the landscape
        tap(KeyEvent.VK_RIGHT);                   // the first theme instead of a changing one
        tap(KeyEvent.VK_UP);                      // rounds to win
        tap(KeyEvent.VK_RIGHT);
        tap(KeyEvent.VK_DOWN);
        tap(KeyEvent.VK_DOWN);
        tap(KeyEvent.VK_DOWN);                    // round the bottom to the number of players
        tap(KeyEvent.VK_RIGHT);
        assertEquals(3, setup.card().players());
        tap(KeyEvent.VK_ENTER);
        ArtilleryGame.PlayScene play = assertInstanceOf(ArtilleryGame.PlayScene.class, game.top());
        assertEquals(3, play.match().setup().seats().size());
        assertEquals(3, play.match().setup().toWin());
        assertEquals(Theme.VALLEY, play.match().setup().theme());
        assertEquals(play.match().setup(), settings.setup());
    }

    @Test
    void escapeOnTheCardGoesBackToTheTitle() {
        setup();
        tap(KeyEvent.VK_ESCAPE);
        assertInstanceOf(ArtilleryGame.TitleScene.class, game.top());
    }

    @Test
    void theMouseChoosesRowsAndStarts() {
        steps(1);
        click(320, 200);
        ArtilleryGame.SetupScene setup = assertInstanceOf(ArtilleryGame.SetupScene.class, game.top());
        SetupCard card = setup.card();
        double top = Hud.cardTop(card.lines().size());
        click(320, top + 5);                       // the players row: selected
        assertEquals(0, card.selected());
        click(320, top + 5);                       // and again: changed
        assertEquals(3, card.players());
        double start = top + (card.lines().size() - 1) * Hud.CARD_ROW + 5;
        click(320, start);
        assertInstanceOf(ArtilleryGame.PlayScene.class, game.top());
    }

    @Test
    void theArrowsAimTabChangesTheWeaponAndSpaceFires() {
        ArtilleryGame.PlayScene play = personAgainstComputer();
        seconds(1);
        Tank tank = play.battle().current();
        assertEquals(Control.PERSON, tank.seat().control());
        double angle = tank.angle(), power = tank.power();
        keyboard.pressed(KeyEvent.VK_LEFT, now[0]);
        keyboard.pressed(KeyEvent.VK_UP, now[0]);
        seconds(0.6);
        keyboard.released(KeyEvent.VK_LEFT, now[0]);
        keyboard.released(KeyEvent.VK_UP, now[0]);
        steps(1);
        assertNotEquals(angle, tank.angle());
        assertTrue(tank.power() > power, "up adds power");
        tap(KeyEvent.VK_TAB);
        assertEquals(Weapon.HEAVY, tank.weapon());
        tap(KeyEvent.VK_Q);
        assertEquals(Weapon.SHELL, tank.weapon());
        tap(KeyEvent.VK_SPACE);
        assertEquals(Battle.Phase.FLIGHT, play.battle().phase());
    }

    @Test
    void holdingTheMousePointsTheBarrelAndLettingGoFires() {
        ArtilleryGame.PlayScene play = personAgainstComputer();
        seconds(1);
        Tank tank = play.battle().current();
        mouse.moved(tank.pivotX() + 80, tank.pivotY() - 80);
        mouse.pressed(MouseEvent.BUTTON1);
        steps(2);
        assertEquals(45, tank.angle(), 0.5);
        assertEquals(Math.hypot(80, 80) / 1.6, tank.power(), 0.5);
        mouse.released(MouseEvent.BUTTON1);
        steps(1);
        assertEquals(Battle.Phase.FLIGHT, play.battle().phase());
    }

    @Test
    void pFreezesTheBattleAndEscapeLeavesIt() {
        ArtilleryGame.PlayScene play = personAgainstComputer();
        seconds(0.5);
        tap(KeyEvent.VK_P);
        assertInstanceOf(ArtilleryGame.PauseScene.class, game.top());
        double time = play.battle().time();
        seconds(1);
        assertEquals(time, play.battle().time());
        tap(KeyEvent.VK_ENTER);
        seconds(0.5);
        assertTrue(play.battle().time() > time);
        tap(KeyEvent.VK_P);
        tap(KeyEvent.VK_ESCAPE);
        assertInstanceOf(ArtilleryGame.TitleScene.class, game.top());
    }

    @Test
    void computersPlayAMatchToTheEndWithReplayAndCards() {
        game.play(new Match.Setup(List.of(new Seat(0, Control.HARD), new Seat(1, Control.HARD)), 1, Theme.DESERT), 3);
        boolean replayed = false;
        for (int i = 0; i < 120 * 300 && !(game.top() instanceof ArtilleryGame.RoundScene); i++) {
            replayed |= game.top() instanceof ArtilleryGame.ReplayScene;
            steps(1);
        }
        assertTrue(replayed, "the decisive shot was shown again");
        assertInstanceOf(ArtilleryGame.RoundScene.class, game.top());
        seconds(1);
        tap(KeyEvent.VK_ENTER);
        assertInstanceOf(ArtilleryGame.MatchScene.class, game.top());
        assertEquals(1, results.size());
        seconds(1.5);
        tap(KeyEvent.VK_ENTER);
        assertInstanceOf(ArtilleryGame.TitleScene.class, game.top());
    }

    @Test
    void theDemoOnTheTitleFightsOn() {
        ArtilleryGame.TitleScene title = assertInstanceOf(ArtilleryGame.TitleScene.class, game.top());
        seconds(20);
        assertTrue(title.demo().turnNumber() > 1, "the computers took turns");
    }

    @Test
    void everySceneDrawsWorldAndOverlay() {
        BufferedImage image = new BufferedImage((int) Field.WIDTH, (int) Field.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        seconds(2);
        draw(g);
        setup();
        draw(g);
        tap(KeyEvent.VK_ENTER);
        seconds(1.5);
        draw(g);
        tap(KeyEvent.VK_P);
        draw(g);
        for (Theme theme : Theme.values()) {
            game.play(new Match.Setup(List.of(new Seat(0, Control.HARD), new Seat(1, Control.HARD),
                    new Seat(2, Control.EASY), new Seat(3, Control.NORMAL)), 1, theme), theme.ordinal());
            for (int i = 0; i < 8; i++) {
                seconds(1);
                draw(g);
            }
        }
        g.dispose();
        assertTrue(image.getRGB(10, 10) != 0, "something was drawn");
    }

    private void draw(Graphics2D g) {
        game.render(g, 0.5);
        game.renderOverlay(g, 0.5);
    }
}
