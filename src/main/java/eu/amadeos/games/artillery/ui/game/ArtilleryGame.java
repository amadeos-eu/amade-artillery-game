package eu.amadeos.games.artillery.ui.game;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Controls;
import eu.amadeos.gameengine.input.Gamepad;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.gameengine.input.Mouse;
import eu.amadeos.gameengine.loop.GameLoop;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.scene.Scene;
import eu.amadeos.gameengine.scene.SceneStack;
import eu.amadeos.gameengine.swing.GameCanvas;
import eu.amadeos.games.artillery.domain.Battle;
import eu.amadeos.games.artillery.domain.Computer;
import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Field;
import eu.amadeos.games.artillery.domain.Input;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Tank;
import eu.amadeos.games.artillery.domain.Theme;
import eu.amadeos.games.artillery.domain.Weapon;
import eu.amadeos.games.artillery.repository.ArtillerySettings;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * The running game: the loop, the controls, the effects and the scenes (the title with the computers
 * fighting, the setup card, a round being played, the replay of the decisive shot, the pause, the end of a
 * round and of the match). Everything here runs on the game loop thread, except the {@link Arena}
 * methods, which only post commands to it.
 */
public final class ArtilleryGame implements Arena {
    enum Action { LEFT, RIGHT, UP, DOWN, NEXT_WEAPON, PREVIOUS_WEAPON, FIRE, PAUSE, BACK, OK }

    static final int UPDATES_PER_SECOND = 120;
    static final int FRAMES_PER_SECOND = 60;
    static final double STEP = 1.0 / UPDATES_PER_SECOND;
    /** How long a decided round stays on the screen before the replay or the card. */
    static final double OVER_SECONDS = 1.2;
    /** The replay runs at this part of the real speed. */
    static final double REPLAY_SPEED = 0.4;

    private final Keyboard keyboard;
    private final Mouse mouse;
    private final Gamepads gamepads;
    private final Messages msg;
    private final Mixer mixer;
    private final ArtillerySettings settings;
    private final Painter painter = new Painter();
    private final Hud hud;
    private final Effects effects;
    private final SceneStack scenes = new SceneStack();
    private final Controls<Action> controls;
    private final Computer computer = new Computer(new SeededRandom(SeededRandom.freshSeed()));
    private final Executor ui;
    private volatile Consumer<Match.Result> onMatchOver = result -> { };
    private GameLoop loop;
    private long seed = SeededRandom.freshSeed();

    @Inject
    public ArtilleryGame(Keyboard keyboard, Mouse mouse, Gamepads gamepads, Messages msg, ArtillerySettings settings,
                         Mixer mixer) {
        this(keyboard, mouse, gamepads, msg, settings, mixer, SwingUtilities::invokeLater);
    }

    ArtilleryGame(Keyboard keyboard, Mouse mouse, Gamepads gamepads, Messages msg, ArtillerySettings settings, Mixer mixer,
                  Executor ui) {
        this.keyboard = keyboard;
        this.mouse = mouse;
        this.gamepads = gamepads;
        this.msg = msg;
        this.mixer = mixer;
        this.settings = settings;
        this.ui = ui;
        this.hud = new Hud(msg);
        this.effects = new Effects(mixer);
        // --8<-- [start:controls]
        controls = new Controls<>(Action.class, keyboard)
                .bind(Action.LEFT, settings.keys("left", KeyEvent.VK_LEFT, KeyEvent.VK_A))
                .bind(Action.RIGHT, settings.keys("right", KeyEvent.VK_RIGHT, KeyEvent.VK_D))
                .bind(Action.UP, settings.keys("up", KeyEvent.VK_UP, KeyEvent.VK_W))
                .bind(Action.DOWN, settings.keys("down", KeyEvent.VK_DOWN, KeyEvent.VK_S))
                .bind(Action.NEXT_WEAPON, settings.keys("weapon", KeyEvent.VK_TAB, KeyEvent.VK_E))
                .bind(Action.PREVIOUS_WEAPON, settings.keys("previous-weapon", KeyEvent.VK_Q))
                .bind(Action.FIRE, settings.keys("fire", KeyEvent.VK_SPACE))
                .bind(Action.PAUSE, settings.keys("pause", KeyEvent.VK_P, KeyEvent.VK_ESCAPE))
                .bind(Action.BACK, KeyEvent.VK_ESCAPE)
                .bind(Action.OK, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
        // Players take turns, so any gamepad plays the tank whose turn it is.
        for (Gamepad pad : gamepads.pads()) {
            controls.bind(Action.LEFT, pad, Gamepad.Button.LEFT)
                    .bind(Action.RIGHT, pad, Gamepad.Button.RIGHT)
                    .bind(Action.UP, pad, Gamepad.Button.UP)
                    .bind(Action.DOWN, pad, Gamepad.Button.DOWN)
                    .bind(Action.NEXT_WEAPON, pad, Gamepad.Button.X, Gamepad.Button.Y)
                    .bind(Action.PREVIOUS_WEAPON, pad, Gamepad.Button.B)
                    .bind(Action.FIRE, pad, Gamepad.Button.A)
                    .bind(Action.PAUSE, pad, Gamepad.Button.START)
                    .bind(Action.BACK, pad, Gamepad.Button.BACK)
                    .bind(Action.OK, pad, Gamepad.Button.A, Gamepad.Button.START);
        }
        // --8<-- [end:controls]
        scenes.push(new TitleScene());
    }

    /** Starts the loop; the canvas draws the scenes' worlds and then their overlays. */
    public void start(GameCanvas canvas) {
        loop = new GameLoop(UPDATES_PER_SECOND, FRAMES_PER_SECOND, this::update, canvas::render);
        loop.start();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }

    void update(double seconds) {
        keyboard.poll();
        mouse.poll();
        gamepads.poll();
        scenes.update(seconds);
    }

    public void render(Graphics2D g, double alpha) {
        scenes.render(g, alpha);
    }

    public void renderOverlay(Graphics2D g, double alpha) {
        scenes.renderOverlay(g, alpha);
    }

    // ---- Arena: called on the Swing thread, carried out on the loop thread -----------------------

    @Override
    public void showTitle() {
        run(() -> scenes.replace(new TitleScene()));
    }

    @Override
    public void newGame() {
        run(() -> {
            if (!(scenes.top() instanceof SetupScene)) scenes.replace(new SetupScene(new TitleScene()));
        });
    }

    @Override
    public void pause() {
        run(() -> {
            if (scenes.top() instanceof PlayScene) scenes.push(new PauseScene());
        });
    }

    @Override
    public void setSound(boolean on) {
        mixer.setMuted(!on);
    }

    @Override
    public String gamepads() {
        return msg.format("about.gamepads", gamepads.backend(), gamepads.connected());
    }

    @Override
    public void onMatchOver(Consumer<Match.Result> listener) {
        onMatchOver = listener;
    }

    private void run(Runnable command) {
        if (loop != null && loop.running()) loop.post(command);
        else command.run();
    }

    // ---- for tests -------------------------------------------------------------------------------

    Scene top() {
        return scenes.top();
    }

    Effects effects() {
        return effects;
    }

    /** Plays a match at once, without the title and the card; the seed makes the landscapes. */
    PlayScene play(Match.Setup setup, long seed) {
        PlayScene play = new PlayScene(new Match(setup, new SeededRandom(seed)));
        scenes.replace(play);
        return play;
    }

    // ---- the scenes ------------------------------------------------------------------------------

    /** Computers fight on the landscapes one after another, without a sound, behind the title. */
    final class TitleScene implements Scene {
        private Battle demo;
        private int round;
        private double time;
        private double over;

        TitleScene() {
            restart();
        }

        private void restart() {
            Theme theme = Theme.values()[round++ % Theme.values().length];
            List<Seat> seats = List.of(new Seat(0, Control.NORMAL), new Seat(1, Control.HARD), new Seat(2, Control.EASY));
            demo = Battle.create(theme, seats, 0, new SeededRandom(seed + round));
            effects.clear(demo);
            effects.stopMusic();
            over = 0;
        }

        @Override
        public void update(double seconds) {
            time += seconds;
            if (controls.wasPressed(Action.OK) || mouse.wasPressed(MouseEvent.BUTTON1)) {
                scenes.replace(new SetupScene(this));
                return;
            }
            step(seconds);
        }

        void step(double seconds) {
            demo.update(seconds, computer.steer(demo, seconds));
            effects.react(demo, false);
            effects.update(seconds, demo, false);
            if (demo.phase() == Battle.Phase.OVER && (over += seconds) > 2) restart();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, demo, effects, alpha, null);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.title(g, time);
        }

        Battle demo() {
            return demo;
        }
    }

    // --8<-- [start:setup]
    /** The card before a match, over the fighting computers. Enter starts, Esc goes back. */
    final class SetupScene implements Scene {
        private final TitleScene behind;
        private final SetupCard card;

        SetupScene(TitleScene behind) {
            this.behind = behind;
            this.card = new SetupCard(settings.setup());
        }

        @Override
        public void update(double seconds) {
            behind.step(seconds);
            if (controls.wasPressed(Action.BACK)) {
                scenes.replace(behind);
                return;
            }
            if (controls.wasPressed(Action.UP)) card.move(-1);
            if (controls.wasPressed(Action.DOWN)) card.move(1);
            if (controls.wasPressed(Action.LEFT)) card.change(-1);
            if (controls.wasPressed(Action.RIGHT)) card.change(1);
            if (mouse.wasPressed(MouseEvent.BUTTON1)) {
                int row = Hud.setupRowAt(card, mouse.x(), mouse.y());
                if (row >= 0) {
                    boolean again = row == card.selected();
                    card.select(row);
                    if (card.lines().get(row).row() == SetupCard.Row.START) {
                        start();
                        return;
                    }
                    if (again) card.change(1);
                }
            }
            if (keyboard.wasPressed(KeyEvent.VK_ENTER) || controls.wasPressed(Action.FIRE) && pressedOnStart()) start();
        }

        private boolean pressedOnStart() {
            return card.lines().get(card.selected()).row() == SetupCard.Row.START;
        }

        private void start() {
            Match.Setup setup = card.setup();
            settings.setSetup(setup);
            scenes.replace(new PlayScene(new Match(setup, new SeededRandom(seed++))));
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            behind.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.setup(g, card);
        }

        SetupCard card() {
            return card;
        }
    }
    // --8<-- [end:setup]

    // --8<-- [start:play]
    /** A round being played. */
    final class PlayScene implements Scene {
        private final Match match;
        private final Battle battle;
        private boolean aiming;
        private double over;

        PlayScene(Match match) {
            this.match = match;
            this.battle = match.nextRound();
            effects.clear(battle);
            effects.roundTune();
        }

        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.PAUSE)) {
                scenes.push(new PauseScene());
                return;
            }
            Tank tank = battle.current();
            Weapon before = tank.weapon();
            boolean person = !tank.seat().control().computer();
            Input input = person ? person(tank) : computer.steer(battle, seconds);
            step(seconds, input);
            if (person) effects.aiming(tank, true, before);
            if (battle.phase() == Battle.Phase.OVER && (over += seconds) >= OVER_SECONDS) {
                match.roundOver();
                Battle replay = battle.replay();
                Scene card = new RoundScene(this);
                scenes.replace(replay != null ? new ReplayScene(this, replay, battle.replayShot(), card) : card);
                effects.endTune(battle.winner() != null, match.over());
            }
        }

        /** The keys, the gamepad and the mouse of the person whose turn it is. */
        private Input person(Tank tank) {
            if (battle.phase() != Battle.Phase.AIM) {
                aiming = false;
                return Input.NONE;
            }
            // Hold the button, point, let go: the barrel follows the pointer, the distance is the power.
            if (mouse.wasPressed(MouseEvent.BUTTON1)) aiming = true;
            if (aiming) {
                double dx = mouse.x() - tank.pivotX(), dy = tank.pivotY() - mouse.y();
                double angle = Math.toDegrees(Math.atan2(Math.max(0, dy), dx));
                double power = Math.min(100, Math.hypot(dx, dy) / 1.6);
                boolean fire = mouse.wasReleased(MouseEvent.BUTTON1);
                if (fire) aiming = false;
                return Input.aim(angle, power, fire);
            }
            int weapon = controls.wasPressed(Action.NEXT_WEAPON) ? 1 : controls.wasPressed(Action.PREVIOUS_WEAPON) ? -1 : 0;
            return Input.keys(controls.axis(Action.RIGHT, Action.LEFT), controls.axis(Action.DOWN, Action.UP), weapon,
                    controls.wasPressed(Action.FIRE));
        }

        void step(double seconds, Input input) {
            battle.update(seconds, input);
            effects.react(battle, true);
            effects.update(seconds, battle, true);
        }
        // --8<-- [end:play]

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, battle, effects, alpha, aiming ? new Point2D.Double(mouse.x(), mouse.y()) : null);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.labels(g, battle, effects, alpha);
            hud.banners(g, battle, match, effects);
            hud.status(g, battle, match);
        }

        Battle battle() {
            return battle;
        }

        Match match() {
            return match;
        }
    }

    // --8<-- [start:replay]
    /**
     * The shot that decided the round, once more, slower: the battle as it was before it, fired again with
     * the same aim. The rules always take whole steps, so the replay is the same shot to the last crumb of
     * earth; it is only shown slower.
     */
    final class ReplayScene implements Scene {
        private final PlayScene play;
        private final Battle replay;
        private final Scene next;
        private Input shot;
        private double pending;
        private double time;
        private double over;

        ReplayScene(PlayScene play, Battle replay, Input shot, Scene next) {
            this.play = play;
            this.replay = replay;
            this.shot = shot;
            this.next = next;
            effects.clear(replay);
        }

        @Override
        public void update(double seconds) {
            time += seconds;
            if (controls.wasPressed(Action.OK) || mouse.wasPressed(MouseEvent.BUTTON1)) {
                scenes.replace(next);
                return;
            }
            pending += seconds * REPLAY_SPEED;
            while (pending >= STEP) {
                pending -= STEP;
                replay.update(STEP, shot);
                shot = Input.NONE;
                effects.react(replay, true);
                effects.update(STEP, replay, true);
            }
            if (replay.phase() == Battle.Phase.OVER && (over += seconds) > 0.8) scenes.replace(next);
        }
        // --8<-- [end:replay]

        @Override
        public void render(Graphics2D g, double alpha) {
            painter.paint(g, replay, effects, pending / STEP, null);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.labels(g, replay, effects, pending / STEP);
            hud.status(g, replay, play.match);
            hud.replay(g, time);
        }

        Battle battle() {
            return replay;
        }
    }

    /** Drawn over the frozen battle until P, Enter or Start; Esc leaves the match. */
    final class PauseScene implements Scene {
        @Override
        public void update(double seconds) {
            if (controls.wasPressed(Action.BACK)) scenes.replace(new TitleScene());
            else if (controls.wasPressed(Action.PAUSE) || controls.wasPressed(Action.OK)) scenes.pop();
        }

        @Override
        public void render(Graphics2D g, double alpha) {
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.pause(g);
        }
    }

    /** The end of a round: who won it and the table; then the next round, or the end of the match. */
    final class RoundScene implements Scene {
        private final PlayScene play;
        private double shown;

        RoundScene(PlayScene play) {
            this.play = play;
        }

        @Override
        public void update(double seconds) {
            play.step(seconds, Input.NONE);
            shown += seconds;
            if (shown < 0.6) return;          // a short wait, so a key meant for the tank does not skip the card
            if (controls.wasPressed(Action.OK) || mouse.wasPressed(MouseEvent.BUTTON1)) {
                if (play.match.over()) {
                    scenes.replace(new MatchScene(play));
                    Match.Result result = play.match.result();
                    Consumer<Match.Result> listener = onMatchOver;
                    ui.execute(() -> listener.accept(result));
                } else {
                    scenes.replace(new PlayScene(play.match));
                }
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            play.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.roundOver(g, play.match, play.battle.winner());
        }
    }

    /** The end of the match: the winner and the table stay on the screen, Enter leads back to the title. */
    final class MatchScene implements Scene {
        private final PlayScene play;
        private double shown;

        MatchScene(PlayScene play) {
            this.play = play;
        }

        @Override
        public void update(double seconds) {
            play.step(seconds, Input.NONE);
            shown += seconds;
            if (shown > 1 && (controls.wasPressed(Action.OK) || mouse.wasPressed(MouseEvent.BUTTON1))) {
                scenes.replace(new TitleScene());
            }
        }

        @Override
        public void render(Graphics2D g, double alpha) {
            play.render(g, alpha);
        }

        @Override
        public void renderOverlay(Graphics2D g, double alpha) {
            hud.matchOver(g, play.match);
        }

        Match match() {
            return play.match;
        }
    }
}
