package eu.amadeos.games.artillery.domain;

import eu.amadeos.gameengine.physics.Ballistics;
import eu.amadeos.gameengine.physics.Projectile;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.terrain.Ground;
import eu.amadeos.gameengine.terrain.Landscape;
import eu.amadeos.gameengine.turn.TurnOrder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * One round on one landscape: the ground, the tanks, the wind, whose turn it is, the shells in the air,
 * the blasts and the earth that falls after them. Without a user interface and without a clock of its
 * own: {@link #update} moves it a step and says in {@link #events()} what happened.
 *
 * <p>A turn goes through three phases: the tank whose turn it is <b>aims</b> (the player turns the barrel,
 * sets the power, picks a weapon and fires), its shells <b>fly</b>, and then the battlefield <b>settles</b>:
 * blasts fade, earth falls, tanks fall with it, wrecks blow up. When everything is quiet, the next tank
 * gets the wind of its turn, or the round is over.
 */
public final class Battle {
    public enum Phase { AIM, FLIGHT, SETTLE, OVER }

    /** A shell in the air, who fired it, and the line it leaves. */
    public record Shell(Projectile projectile, Weapon weapon, Tank owner, boolean traced) {
    }

    /** A blast, as long as it is seen. */
    public static final class Blast {
        private final double x;
        private final double y;
        private final double radius;
        private final Weapon weapon;
        private double age;

        Blast(double x, double y, double radius, Weapon weapon) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.weapon = weapon;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public double radius() {
            return radius;
        }

        /** {@code null} for a wreck blowing up. */
        public Weapon weapon() {
            return weapon;
        }

        /** 0 when it starts, 1 when it is gone. */
        public double progress() {
            return Math.min(1, age / Field.EXPLOSION_SECONDS);
        }
    }

    private static final double STEP = 1 / 120.0;
    /** The dotted line keeps a point every so many steps. */
    private static final int TRACE_EVERY = 4;

    private final Theme theme;
    private final Ground ground;
    private final List<Tank> tanks;
    private final TurnOrder<Tank> turns;
    private final SeededRandom random;
    private final double gravity;
    private int wind;
    private Ballistics air;
    private Phase phase = Phase.AIM;
    private double phaseTime;
    private double time;
    private double quiet;
    private final List<Shell> shells = new ArrayList<>();
    private final List<Blast> blasts = new ArrayList<>();
    private final List<Tank> wrecks = new ArrayList<>();
    private final List<Double> wreckTimers = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();
    private Tank shooter;
    private int traceSteps;
    private int lastTurn;
    private double turnHeld;
    private double powerHeld;
    private int turnNumber;
    private Battle beforeShot;
    private Input shot;
    private boolean destroyedThisTurn;
    private Battle decisive;
    private Input decisiveShot;

    private Battle(Theme theme, Ground ground, List<Tank> tanks, int first, SeededRandom random, int wind) {
        this.theme = theme;
        this.ground = ground;
        this.tanks = tanks;
        this.turns = new TurnOrder<>(tanks, Tank::alive, first);
        this.random = random;
        this.gravity = Field.GRAVITY * theme.gravity();
        this.wind = wind;
        this.air = air(wind);
    }

    // --8<-- [start:create]
    /**
     * A new round: a landscape from the theme and the random numbers, the tanks of the {@code seats} on
     * shelves of level ground spread over it, and {@code first} (an index into {@code seats}) begins.
     */
    public static Battle create(Theme theme, List<Seat> seats, int first, SeededRandom random) {
        int n = seats.size();
        int samples = 129;
        double spacing = Field.WIDTH / (samples - 1);
        double[] line = Landscape.generate(samples, theme.base(), theme.amplitude(), theme.roughness(), random);
        if (theme == Theme.VALLEY) Landscape.bump(line, samples / 2.0, 14, -70);
        if (theme == Theme.MOON) craters(line, random);
        Landscape.clamp(line, Field.TOP + 110, Field.HEIGHT - 30);
        // Each tank gets a slot of the field and a place somewhere in the middle of it; the seats are shuffled
        // over the slots, so the first player does not always stand on the left.
        List<Seat> order = new ArrayList<>(seats);
        for (int i = order.size() - 1; i > 0; i--) java.util.Collections.swap(order, i, random.nextInt(i + 1));
        double slot = (Field.WIDTH - 40) / n;
        double[] places = new double[n];
        for (int i = 0; i < n; i++) {
            places[i] = 20 + slot * i + slot * (0.25 + random.nextDouble() * 0.5);
            Landscape.flatten(line, (int) Math.round(places[i] / spacing), 3);
        }
        Ground ground = new Ground(Field.WIDTH, Field.HEIGHT, Field.CELL);
        ground.fill(Landscape.sample(line, spacing), (x, depth) -> depth < Earth.TOP_DEPTH ? Earth.TOP : Earth.SOIL);
        List<Tank> tanks = new ArrayList<>();
        for (Seat seat : seats) {
            double x = places[order.indexOf(seat)];
            tanks.add(new Tank(seat, x, ground.surface(x - Field.TANK_WIDTH / 2 + 3, x + Field.TANK_WIDTH / 2 - 3)));
        }
        int wind = theme.windy() ? random.nextInt(-Field.MAX_WIND, Field.MAX_WIND + 1) : 0;
        Battle battle = new Battle(theme, ground, tanks, first, random, wind);
        battle.events.add(new Event.TurnStarted(battle.current(), wind));
        return battle;
    }
    // --8<-- [end:create]

    /** Old craters of the moon: bowls with a rim, pressed into the line before the shelves are made. */
    private static void craters(double[] line, SeededRandom random) {
        int count = 3 + random.nextInt(3);
        for (int c = 0; c < count; c++) {
            double centre = random.nextDouble() * line.length;
            double width = 3 + random.nextDouble() * 5;
            double depth = 12 + random.nextDouble() * 18;
            for (int i = 0; i < line.length; i++) {
                double d = Math.abs(i - centre) / width;
                if (d < 1) line[i] += depth * (1 - d * d);
                else if (d < 1.5) line[i] -= depth * 0.25 * (1 - (d - 1) * 2);
            }
        }
    }

    /** A copy to replay a shot with: only at the start of a turn, while nothing flies. */
    private Battle copy() {
        List<Tank> copies = new ArrayList<>();
        for (Tank t : tanks) copies.add(t.copy());
        Battle b = new Battle(theme, ground.copy(), copies, tanks.indexOf(current()), random.copy(), wind);
        b.turnNumber = turnNumber;
        b.time = time;
        return b;
    }

    private Ballistics air(int wind) {
        return new Ballistics(gravity, wind * Field.WIND_PUSH, 0, Field.CELL);
    }

    // --8<-- [start:update]
    /** One step: the input moves the tank whose turn it is, then shells fly, blasts fade, earth and tanks fall. */
    public void update(double seconds, Input input) {
        events.clear();
        time += seconds;
        phaseTime += seconds;
        if (phase == Phase.AIM) aim(seconds, input);
        if (phase == Phase.FLIGHT) fly(seconds);
        boolean busy = aftermath(seconds);
        if (phase == Phase.FLIGHT && shells.isEmpty()) enter(Phase.SETTLE);
        if (phase == Phase.SETTLE) {
            quiet = busy ? 0 : quiet + seconds;
            if (quiet >= Field.CALM_SECONDS) endTurn();
        }
    }
    // --8<-- [end:update]

    private void enter(Phase next) {
        phase = next;
        phaseTime = 0;
        quiet = 0;
    }

    // --8<-- [start:aim]
    private void aim(double seconds, Input input) {
        Tank tank = current();
        double angle = tank.angle(), power = tank.power();
        if (!Double.isNaN(input.angle())) angle = input.angle();
        if (!Double.isNaN(input.setPower())) power = input.setPower();
        // A tap moves one degree or one point; held, the barrel turns on by itself after a moment.
        if (input.turn() != 0) {
            if (input.turn() != lastTurn) {
                angle += input.turn();
                turnHeld = 0;
            } else if ((turnHeld += seconds) > Field.HOLD_DELAY) {
                angle += input.turn() * Field.TURN_SPEED * seconds;
            }
        }
        if (input.power() != 0) {
            if (powerHeld == 0) power += input.power();
            powerHeld += seconds;
            if (powerHeld > Field.HOLD_DELAY) power += input.power() * Field.POWER_SPEED * seconds;
        } else {
            powerHeld = 0;
        }
        lastTurn = input.turn();
        tank.aim(angle, power);
        if (input.weapon() != 0) tank.cycleWeapon(input.weapon());
        if (input.fire()) {
            beforeShot = copy();
            shot = Input.aim(tank.angle(), tank.power(), true);
            beforeShot.current().select(tank.weapon());
            fire(tank);
        }
    }
    // --8<-- [end:aim]

    /** The speed of a shell at a power from 0 to 100; on the moon slower, so it flies as far, but floats. */
    public double speed(double power) {
        return power / 100 * Field.MAX_SPEED * Math.sqrt(theme.gravity());
    }

    // --8<-- [start:fire]
    private void fire(Tank tank) {
        Weapon weapon = tank.weapon();
        double angle = tank.angle();
        double x = tank.muzzleX(angle), y = tank.muzzleY(angle);
        shooter = tank;
        destroyedThisTurn = false;
        tank.lastShot().clear();
        traceSteps = 0;
        events.add(new Event.Fired(tank, weapon, x, y));
        for (int i = 0; i < weapon.shells(); i++) {
            double a = angle + (i - (weapon.shells() - 1) / 2.0) * Weapon.SPREAD;
            Projectile p = Projectile.launch(x, y, a, speed(tank.power()));
            shells.add(new Shell(p, weapon, tank, i == weapon.shells() / 2));
        }
        tank.used();
        // A barrel buried in earth fires into it.
        if (ground.solid(x, y)) {
            for (Shell s : shells) land(s, x, y);
            shells.clear();
        }
        enter(Phase.FLIGHT);
    }
    // --8<-- [end:fire]

    // --8<-- [start:fly]
    private void fly(double seconds) {
        traceSteps++;
        Ballistics.Solid solid = this::solid;
        for (Iterator<Shell> it = shells.iterator(); it.hasNext(); ) {
            Shell s = it.next();
            Projectile p = s.projectile();
            Ballistics.Hit hit = air.step(p, seconds, solid);
            if (s.traced() && traceSteps % TRACE_EVERY == 0) s.owner().lastShot().add(new double[]{p.x, p.y});
            if (hit != null) {
                land(s, hit.x(), hit.y());
                it.remove();
            } else if (p.x < 0 || p.x > Field.WIDTH || phaseTime > Field.MAX_FLIGHT) {
                it.remove();                                   // gone, over the edge of the world
            }
        }
    }

    /** What a shell hits: earth, a tank, the bottom of the world. */
    public boolean solid(double x, double y) {
        if (y >= Field.HEIGHT || ground.solid(x, y)) return true;
        for (Tank t : tanks) if (t.alive() && t.contains(x, y)) return true;
        return false;
    }
    // --8<-- [end:fly]

    // --8<-- [start:land]
    private void land(Shell shell, double x, double y) {
        Weapon w = shell.weapon();
        if (shell.traced()) shell.owner().lastShot().add(new double[]{x, y});
        if (w.heapsEarth()) {
            ground.heap(x, y, w.radius(), Earth.DIRT);
            events.add(new Event.Heaped(x, y, w.radius()));
            return;
        }
        blast(x, y, w.radius(), w.damage(), w, shell.owner());
    }

    private void blast(double x, double y, double radius, int damage, Weapon weapon, Tank by) {
        ground.recolour(x, y, radius + 2, Earth.SCORCHED);
        ground.carve(x, y, radius);
        blasts.add(new Blast(x, y, radius, weapon));
        events.add(new Event.Exploded(x, y, radius, weapon));
        for (Tank t : tanks) {
            if (!t.alive()) continue;
            double reach = radius + Field.TANK_WIDTH / 2 - 2;
            double d = Math.hypot(t.x() - x, t.centreY() - y);
            if (d >= reach) continue;
            int hurt = (int) Math.round(damage * (1 - Math.max(0, d - 3) / reach));
            hurt(t, hurt, by);
        }
    }
    // --8<-- [end:land]

    private void hurt(Tank t, int damage, Tank by) {
        if (damage <= 0 || !t.alive()) return;
        int dealt = Math.min(damage, t.health());
        boolean destroyed = t.damage(damage);
        events.add(new Event.Hit(t, dealt));
        if (by != null && by != t) by.dealt(dealt);
        if (destroyed) {
            destroyedThisTurn = true;
            events.add(new Event.Destroyed(t));
            if (by != null && by != t) by.killed();
            wrecks.add(t);
            wreckTimers.add(Field.WRECK_DELAY);
        }
    }

    // --8<-- [start:aftermath]
    /** Blasts fade, wrecks blow up, earth falls, tanks fall with it. Returns whether anything is still going on. */
    private boolean aftermath(double seconds) {
        boolean busy = false;
        for (Iterator<Blast> it = blasts.iterator(); it.hasNext(); ) {
            Blast b = it.next();
            b.age += seconds;
            if (b.age >= Field.EXPLOSION_SECONDS) it.remove();
            else busy = true;
        }
        for (int i = 0; i < wrecks.size(); i++) {
            double left = wreckTimers.get(i) - seconds;
            if (left > 0) {
                wreckTimers.set(i, left);
                busy = true;
                continue;
            }
            Tank wreck = wrecks.remove(i);
            wreckTimers.remove(i);
            i--;
            blast(wreck.x(), wreck.centreY(), Field.WRECK_RADIUS, Field.WRECK_DAMAGE, null, null);
            busy = true;
        }
        if (ground.settle(Field.SETTLE_PASSES)) busy = true;
        for (Tank t : tanks) {
            double half = Field.TANK_WIDTH / 2 - 3;
            double depth = t.fall(ground.surface(t.x() - half, t.x() + half), gravity, seconds);
            if (t.falling()) busy = true;
            if (depth >= 0) {
                int damage = depth > Field.SAFE_FALL ? (int) Math.round((depth - Field.SAFE_FALL) * Field.FALL_DAMAGE) : 0;
                events.add(new Event.Fell(t, depth, damage));
                hurt(t, damage, shooter);
            }
        }
        return busy || !shells.isEmpty();
    }
    // --8<-- [end:aftermath]

    private void endTurn() {
        if (destroyedThisTurn && beforeShot != null) {
            decisive = beforeShot;
            decisiveShot = shot;
        }
        beforeShot = null;
        if (turns.decided()) {
            enter(Phase.OVER);
            events.add(new Event.RoundOver(turns.winner()));
            return;
        }
        if (theme.windy()) {
            wind = Math.max(-Field.MAX_WIND, Math.min(Field.MAX_WIND,
                    wind + random.nextInt(-Field.WIND_CHANGE, Field.WIND_CHANGE + 1)));
            air = air(wind);
        }
        turnNumber++;
        Tank next = turns.next();
        lastTurn = 0;
        turnHeld = powerHeld = 0;
        enter(Phase.AIM);
        events.add(new Event.TurnStarted(next, wind));
    }

    /**
     * The shot that destroyed a tank last, ready to be fired again: the battle as it was just before it, and
     * the input that fired it. {@code null} if no tank has been destroyed by a shot yet.
     */
    public Battle replay() {
        return decisive == null ? null : decisive.copy();
    }

    public Input replayShot() {
        return decisiveShot;
    }

    // ---- what the user interface and the computer read --------------------------------------------

    public Tank current() {
        return turns.current();
    }

    public List<Tank> tanks() {
        return tanks;
    }

    public Ground ground() {
        return ground;
    }

    public Theme theme() {
        return theme;
    }

    public int wind() {
        return wind;
    }

    /** The air as it is this turn: gravity and wind. */
    public Ballistics air() {
        return air;
    }

    /** The air with another wind, for a computer that guesses it. */
    public Ballistics air(double guessedWind) {
        return new Ballistics(gravity, guessedWind * Field.WIND_PUSH, 0, Field.CELL);
    }

    public double gravity() {
        return gravity;
    }

    public Phase phase() {
        return phase;
    }

    public double phaseTime() {
        return phaseTime;
    }

    public double time() {
        return time;
    }

    /** How many turns have passed in this round. */
    public int turnNumber() {
        return turnNumber;
    }

    public List<Shell> shells() {
        return shells;
    }

    public List<Blast> blasts() {
        return blasts;
    }

    /** What happened in the last step. */
    public List<Event> events() {
        return events;
    }

    /** The tank left, once the round is over; {@code null} for none. */
    public Tank winner() {
        return phase == Phase.OVER ? turns.winner() : null;
    }

    /** The step the battle is meant to run at; the computer's trials use it too. */
    public static double step() {
        return STEP;
    }
}
