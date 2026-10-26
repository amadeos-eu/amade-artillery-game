package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.fx.Particles;
import eu.amadeos.gameengine.physics.Projectile;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.games.artillery.domain.Battle;
import eu.amadeos.games.artillery.domain.Earth;
import eu.amadeos.games.artillery.domain.Event;
import eu.amadeos.games.artillery.domain.Tank;
import eu.amadeos.games.artillery.domain.Weapon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * What the events of a step look and sound like: the muzzle flash and smoke of a shot, the whistle of a
 * falling shell, fire, smoke and clods of earth from a blast, the damage rising from a hit tank, a wreck
 * smoking. The rules know nothing of this.
 */
final class Effects {
    /** Fire, smoke, sparks, white, and the earth (set per landscape). */
    static final int FIRE = 0, SMOKE = 1, SPARK = 2, WHITE = 3, EARTH = 4, TOP = 5;
    static final double DAMAGE_SECONDS = 1.4;
    static final double BANNER_SECONDS = 1.8;

    /** Damage rising from a tank. */
    record Damage(int amount, double x, double y, double born) {
    }

    private final SoundBank sounds = new SoundBank();
    private final Music music = new Music();
    private final Mixer mixer;
    private final SeededRandom random = new SeededRandom(3);
    private final Color[] palette = {new Color(0xFF8A2A), new Color(0x5E5E66), new Color(0xFFF0B0), new Color(0xF4F4F0),
            new Color(0x5F4A2E), new Color(0x8FB84A)};
    private Particles particles = new Particles(900, palette);
    private final List<Damage> damage = new ArrayList<>();
    private final Set<Projectile> whistling = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<Tank> wrecks = new ArrayList<>();
    private double time;
    private double shake;
    private Tank turn;
    private double turnShown = -10;
    private int lastTick = Integer.MIN_VALUE;

    Effects(Mixer mixer) {
        this.mixer = mixer;
    }

    double time() {
        return time;
    }

    /** A new battle, or the title: nothing left from before, and the earth in the battle's colours. */
    void clear(Battle battle) {
        int[] earth = Look.earth(battle.theme());
        palette[EARTH] = new Color(earth[Earth.SOIL]);
        palette[TOP] = new Color(earth[Earth.TOP]);
        particles = new Particles(900, palette);
        damage.clear();
        whistling.clear();
        wrecks.clear();
        shake = 0;
        turn = battle.current();
        turnShown = time;
        lastTick = Integer.MIN_VALUE;
    }

    void stopMusic() {
        mixer.stopMusic();
    }

    void roundTune() {
        mixer.playMusic(music.round, false);
    }

    void endTune(boolean somebodyWon, boolean match) {
        mixer.playMusic(!somebodyWon ? music.nobody : match ? music.matchWon : music.roundWon, false);
    }

    /** How far the picture shakes now, after a blast, in units. */
    double shake() {
        return shake;
    }

    // --8<-- [start:react]
    /** Shows and, if {@code audible}, plays what happened in the last step. */
    void react(Battle battle, boolean audible) {
        for (Event event : battle.events()) {
            switch (event) {
                case Event.TurnStarted(Tank tank, int wind) -> {
                    turn = tank;
                    turnShown = time;
                    play(audible, sounds.turn);
                }
                case Event.Fired(Tank tank, Weapon weapon, double x, double y) -> {
                    burst(x, y, 10, FIRE, 60, -10, 0.15);
                    burst(x, y, 8, SMOKE, 30, -15, 0.6);
                    play(audible, switch (weapon) {
                        case HEAVY -> sounds.heavyFire;
                        case TRIPLE -> sounds.tripleFire;
                        case DIRT -> sounds.dirtFire;
                        default -> sounds.fire;
                    });
                }
                case Event.Exploded(double x, double y, double radius, Weapon weapon) -> {
                    int size = (int) radius;
                    burst(x, y, size * 2, FIRE, radius * 6, -20, 0.5);
                    burst(x, y, size, SMOKE, radius * 3, -30, 1.1);
                    burst(x, y, size, SPARK, radius * 9, -40, 0.4);
                    clods(x, y, size * 2, radius * 7);
                    shake = Math.max(shake, radius / 5);
                    play(audible, radius > 20 ? sounds.bigExplosion : sounds.explosion);
                }
                case Event.Heaped(double x, double y, double radius) -> {
                    clods(x, y - radius / 2, (int) radius * 2, radius * 3);
                    play(audible, sounds.heap);
                }
                case Event.Hit(Tank tank, int amount) -> {
                    damage.add(new Damage(amount, tank.x(), tank.y() - 20, time));
                    play(audible, sounds.clank);
                }
                case Event.Destroyed(Tank tank) -> wrecks.add(tank);
                case Event.Fell(Tank tank, double depth, int hurt) -> {
                    burst(tank.x(), tank.y(), (int) Math.min(16, 4 + depth), EARTH, 30, -20, 0.5);
                    if (depth > 3) play(audible, sounds.thud);
                }
                case Event.RoundOver over -> {
                }
            }
        }
    }
    // --8<-- [end:react]

    private void play(boolean audible, Sound sound) {
        if (audible) mixer.play(sound);
    }

    /** Plays a soft click every five degrees the barrel of a person's tank turns, and when a weapon is loaded. */
    void aiming(Tank tank, boolean audible, Weapon before) {
        int tick = (int) Math.floor(tank.angle() / 5) * 1000 + (int) Math.floor(tank.power() / 5);
        if (lastTick != Integer.MIN_VALUE && tick != lastTick) play(audible, sounds.tick);
        lastTick = tick;
        if (before != tank.weapon()) play(audible, sounds.load);
    }

    // --8<-- [start:whistle]
    /** Every step: particles fly, shells smoke and whistle on their way down, wrecks smoke. */
    void update(double seconds, Battle battle, boolean audible) {
        time += seconds;
        shake = Math.max(0, shake - seconds * 10);
        particles.update(seconds, 60);
        damage.removeIf(d -> time - d.born() > DAMAGE_SECONDS);
        for (Battle.Shell shell : battle.shells()) {
            Projectile p = shell.projectile();
            if (random.nextInt(3) == 0) {
                particles.spawn(p.x, p.y, random.nextDouble() * 6 - 3, random.nextDouble() * 6 - 3, 0.5, 1.2, SMOKE);
            }
            // Past the top of its arc, the shell starts to whistle: high, then lower until it lands.
            if (p.vy > 0 && whistling.add(p)) {
                double fall = Math.max(0, battle.ground().surface(p.x) - p.y);
                double seconds2 = (-p.vy + Math.sqrt(p.vy * p.vy + 2 * battle.gravity() * fall)) / battle.gravity();
                if (shell.traced()) play(audible, SoundBank.whistle(seconds2));
            }
        }
        whistling.removeIf(p -> battle.shells().stream().noneMatch(s -> s.projectile() == p));
        for (Tank wreck : wrecks) {
            if (random.nextInt(4) == 0) {
                particles.spawn(wreck.x() + random.nextDouble() * 8 - 4, wreck.y() - 8, random.nextDouble() * 6 - 3,
                        -14 - random.nextDouble() * 10, 1.4, 2 + random.nextDouble() * 2, SMOKE);
            }
        }
    }
    // --8<-- [end:whistle]

    private void burst(double x, double y, int count, int colour, double speed, double lift, double life) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double v = speed * (0.2 + random.nextDouble() * 0.8);
            particles.spawn(x, y, Math.cos(angle) * v, Math.sin(angle) * v + lift, life * (0.6 + random.nextDouble() * 0.8),
                    1.2 + random.nextDouble() * 2, colour);
        }
    }

    /** Clods of earth thrown up, falling back. */
    private void clods(double x, double y, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI * (0.1 + random.nextDouble() * 0.8);
            double v = speed * (0.3 + random.nextDouble() * 0.7);
            particles.spawn(x, y, Math.cos(angle) * v, Math.sin(angle) * v, 0.8 + random.nextDouble() * 0.6,
                    1 + random.nextDouble() * 1.6, random.nextInt(3) == 0 ? TOP : EARTH);
        }
    }

    void render(Graphics2D g, double alpha) {
        particles.render(g, alpha);
    }

    List<Damage> damage() {
        return damage;
    }

    /** The tank whose turn began last, and how long ago. */
    Tank turn() {
        return turn;
    }

    double sinceTurn() {
        return time - turnShown;
    }

    int particles() {
        return particles.count();
    }
}
