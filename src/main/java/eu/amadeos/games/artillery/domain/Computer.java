package eu.amadeos.games.artillery.domain;

import eu.amadeos.gameengine.ai.ShotSolver;
import eu.amadeos.gameengine.physics.Ballistics;
import eu.amadeos.gameengine.physics.Projectile;
import eu.amadeos.gameengine.random.SeededRandom;

import java.util.Comparator;
import java.util.List;

/**
 * The computer at the controls of a tank. At the start of its turn it picks a target and searches for a
 * shot that hits it, by firing shots in thought with the battle's own ballistics; then it makes a mistake
 * on purpose, as big as its level allows, and turns the barrel there the way a player would, a little in
 * each step, and fires.
 */
public final class Computer {
    /** What the computer decided for a turn. */
    public record Plan(Tank target, double angle, double power, Weapon weapon, double miss) {
    }

    /** How long it seems to think before it moves the barrel, and how fast it turns it then. */
    static final double THINK_SECONDS = 0.6;
    static final double TURN_SPEED = 70;
    static final double POWER_SPEED = 60;
    static final double WEAPON_SECONDS = 0.18;
    static final double STEADY_SECONDS = 0.3;

    private final SeededRandom random;
    private Battle battle;
    private int turn = -1;
    private Plan plan;
    private double time;
    private double ready;
    private double lastWeaponChange;

    public Computer(SeededRandom random) {
        this.random = random;
    }

    // --8<-- [start:steer]
    /** The input of a computer's tank for a step: think, turn the barrel towards the plan, fire. */
    public Input steer(Battle battle, double seconds) {
        if (battle.phase() != Battle.Phase.AIM) return Input.NONE;
        if (battle != this.battle || battle.turnNumber() != turn) {
            this.battle = battle;
            this.turn = battle.turnNumber();
            plan = plan(battle);
            time = 0;
            ready = 0;
            lastWeaponChange = 0;
        }
        time += seconds;
        if (time < THINK_SECONDS) return Input.NONE;
        Tank tank = battle.current();
        if (tank.weapon() != plan.weapon() && tank.has(plan.weapon())) {
            if (time - lastWeaponChange < WEAPON_SECONDS) return Input.NONE;
            lastWeaponChange = time;
            return Input.keys(0, 0, 1, false);
        }
        double angle = toward(tank.angle(), plan.angle(), TURN_SPEED * seconds);
        double power = toward(tank.power(), plan.power(), POWER_SPEED * seconds);
        boolean there = angle == plan.angle() && power == plan.power();
        ready = there ? ready + seconds : 0;
        return Input.aim(angle, power, ready >= STEADY_SECONDS);
    }
    // --8<-- [end:steer]

    private static double toward(double from, double to, double most) {
        if (Math.abs(to - from) <= most) return to;
        return from + Math.signum(to - from) * most;
    }

    // --8<-- [start:plan]
    /** The target, the aim and the weapon for the current tank's turn, with the level's mistake in it. */
    public Plan plan(Battle battle) {
        Tank tank = battle.current();
        Control level = tank.seat().control();
        Tank target = target(battle, tank, level);
        // The easy computer guesses the wind and is often wrong; the others read it from the flag.
        double wind = level == Control.EASY ? battle.wind() + random.nextGaussian() * 2 : battle.wind();
        ShotSolver.Aim aim = aim(battle, tank, target, battle.air(wind));
        double angleError = switch (level) {
            case EASY -> 3;
            case NORMAL -> 1.6;
            default -> 0.7;
        };
        double powerError = switch (level) {
            case EASY -> 3.5;
            case NORMAL -> 2;
            default -> 1;
        };
        double angle = Math.max(0, Math.min(180, aim.angle() + random.nextGaussian() * angleError));
        double power = Math.max(5, Math.min(100, aim.power() + random.nextGaussian() * powerError));
        return new Plan(target, angle, power, weapon(tank, target, level), aim.miss());
    }

    /** Fires shots in thought with the battle's ballistics and its ground, and keeps the one that lands nearest the target. */
    static ShotSolver.Aim aim(Battle battle, Tank tank, Tank target, Ballistics air) {
        Ballistics.Field field = new Ballistics.Field(0, Field.WIDTH, Field.HEIGHT);
        double tx = target.x(), ty = target.centreY();
        ShotSolver.Trial trial = (angle, power) -> {
            Projectile shell = Projectile.launch(tank.muzzleX(angle), tank.muzzleY(angle), angle, battle.speed(power));
            if (battle.ground().solid(shell.x, shell.y)) return Math.hypot(shell.x - tx, shell.y - ty);
            Ballistics.Flight flight = air.fly(shell, Battle.step(), Field.MAX_FLIGHT, field, battle::solid);
            if (flight.ending() != Ballistics.Ending.HIT) return 10_000;
            return Math.hypot(flight.x() - tx, flight.y() - ty);
        };
        boolean right = target.x() > tank.x();
        ShotSolver.Aim aim = right ? ShotSolver.solve(5, 88, 4, 15, 100, 5, 5, trial)
                : ShotSolver.solve(92, 175, 4, 15, 100, 5, 5, trial);
        if (aim.miss() > 20) {
            // Nothing good on its own side: a hill in the way. Try over its own head as well.
            ShotSolver.Aim wide = ShotSolver.solve(5, 175, 5, 15, 100, 5, 5, trial);
            if (wide.miss() < aim.miss()) aim = wide;
        }
        return aim;
    }
    // --8<-- [end:plan]

    private Tank target(Battle battle, Tank tank, Control level) {
        List<Tank> enemies = battle.tanks().stream().filter(t -> t.alive() && t != tank).toList();
        Comparator<Tank> nearest = Comparator.comparingDouble(t -> Math.abs(t.x() - tank.x()));
        return switch (level) {
            case EASY -> enemies.get(random.nextInt(enemies.size()));
            case NORMAL -> enemies.stream().min(nearest).orElseThrow();
            default -> enemies.stream().min(Comparator.comparingInt(Tank::health).thenComparing(nearest)).orElseThrow();
        };
    }

    private Weapon weapon(Tank tank, Tank target, Control level) {
        return switch (level) {
            case HARD -> tank.has(Weapon.HEAVY) && target.health() > Weapon.SHELL.damage() ? Weapon.HEAVY
                    : tank.has(Weapon.TRIPLE) && target.health() <= Weapon.TRIPLE.damage() ? Weapon.TRIPLE : Weapon.SHELL;
            case NORMAL -> tank.has(Weapon.HEAVY) && random.nextInt(3) == 0 ? Weapon.HEAVY
                    : tank.has(Weapon.TRIPLE) && random.nextInt(3) == 0 ? Weapon.TRIPLE : Weapon.SHELL;
            default -> tank.has(Weapon.TRIPLE) && random.nextInt(4) == 0 ? Weapon.TRIPLE : Weapon.SHELL;
        };
    }
}
