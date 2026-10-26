package eu.amadeos.games.artillery.domain;

import eu.amadeos.gameengine.ai.ShotSolver;
import eu.amadeos.gameengine.physics.Ballistics;
import eu.amadeos.gameengine.physics.Projectile;
import eu.amadeos.gameengine.random.SeededRandom;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static eu.amadeos.games.artillery.domain.Battles.STEP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleTest {
    /** Steps the battle with no input until it is a tank's turn again (or over), collecting the events. */
    private static List<Event> settle(Battle battle) {
        List<Event> events = new ArrayList<>();
        for (int i = 0; i < 120 * 30 && battle.phase() != Battle.Phase.AIM && battle.phase() != Battle.Phase.OVER; i++) {
            battle.update(STEP, Input.NONE);
            events.addAll(battle.events());
        }
        return events;
    }

    /** Fires the current tank's loaded weapon so that it lands on the point (or {@code within} of it), and lets everything settle. */
    private static List<Event> shootAt(Battle battle, double x, double y, double within) {
        Tank tank = battle.current();
        Ballistics.Field field = new Ballistics.Field(0, Field.WIDTH, Field.HEIGHT);
        ShotSolver.Aim aim = ShotSolver.solve(5, 175, 4, 10, 100, 5, 6, (a, p) -> {
            Ballistics.Flight f = battle.air().fly(Projectile.launch(tank.muzzleX(a), tank.muzzleY(a), a, battle.speed(p)),
                    STEP, 15, field, battle::solid);
            return f.ending() == Ballistics.Ending.HIT ? Math.hypot(f.x() - x, f.y() - y) : 10_000;
        });
        assertTrue(aim.miss() < within, "there is a shot to " + x + ", " + y + ": " + aim);
        battle.update(STEP, Input.aim(aim.angle(), aim.power(), true));
        List<Event> events = new ArrayList<>(battle.events());
        events.addAll(settle(battle));
        return events;
    }

    @Test
    void tanksStandOnLevelShelvesSpreadOverTheFieldAndTheFirstSeatBegins() {
        Battle battle = Battles.battle(Theme.VALLEY, 4, Control.PERSON, Control.NORMAL, Control.HARD);
        assertEquals(3, battle.tanks().size());
        assertSame(battle.tanks().getFirst(), battle.current());
        List<Double> xs = battle.tanks().stream().map(Tank::x).sorted().toList();
        assertTrue(xs.get(1) - xs.get(0) > 80 && xs.get(2) - xs.get(1) > 80, "spread out: " + xs);
        for (Tank t : battle.tanks()) {
            assertEquals(battle.ground().surface(t.x() - 8), t.y(), 0.6, "on its shelf");
            assertEquals(battle.ground().surface(t.x() + 8), t.y(), 0.6, "level");
            assertEquals(Field.HEALTH, t.health());
        }
    }

    // --8<-- [start:test-aim]
    @Test
    void aTapTurnsTheBarrelOneDegreeAndHoldingTurnsItOn() {
        Battle battle = Battles.battle(Theme.DESERT, 1, Control.PERSON, Control.PERSON);
        Tank tank = battle.current();
        double start = tank.angle();
        battle.update(STEP, Input.keys(1, 0, 0, false));
        battle.update(STEP, Input.NONE);
        assertEquals(start + 1, tank.angle(), 1e-9, "a tap is one degree");
        for (int i = 0; i < 120; i++) battle.update(STEP, Input.keys(1, 0, 0, false));
        double held = tank.angle() - start - 1;
        assertTrue(held > 25 && held < 35, "a second held: the delay, then 40 degrees a second: " + held);
        battle.update(STEP, Input.aim(200, -5, false));
        assertEquals(180, tank.angle(), 1e-9, "never below the horizon");
        assertEquals(0, tank.power(), 1e-9);
    }
    // --8<-- [end:test-aim]

    // --8<-- [start:test-hit]
    @Test
    void aShellOnATankHurtsItAndScoresForTheShooter() {
        Battle battle = Battles.battle(Theme.DESERT, 2, Control.PERSON, Control.PERSON);
        Tank shooter = battle.current();
        Tank target = battle.tanks().get(1);
        List<Event> events = shootAt(battle, target.x(), target.centreY(), Field.TANK_WIDTH / 2);
        int damage = events.stream().filter(e -> e instanceof Event.Hit h && h.tank() == target)
                .mapToInt(e -> ((Event.Hit) e).damage()).sum();
        assertTrue(damage >= 30 && damage <= Weapon.SHELL.damage(), "nearly the full damage: " + damage);
        assertEquals(Field.HEALTH - damage, target.health());
        assertEquals(damage, shooter.damageDealt());
        assertFalse(shooter.lastShot().isEmpty(), "the dotted line of the shot");
        assertSame(target, battle.current(), "and then it is the other tank's turn");
    }
    // --8<-- [end:test-hit]

    @Test
    void aCraterUnderATankMakesItFallAndADeepFallHurts() {
        Battle battle = Battles.battle(Theme.VALLEY, 3, Control.PERSON, Control.PERSON);
        Tank target = battle.tanks().get(1);
        battle.current().cycleWeapon(1);
        assertEquals(Weapon.HEAVY, battle.current().weapon());
        double before = target.y();
        List<Event> events = shootAt(battle, target.x() + 14, target.y() + 2, 3);
        assertTrue(target.y() > before + 5, "the tank came down into the crater: " + before + " -> " + target.y());
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Fell f && f.tank() == target && f.depth() > 5), events.toString());
    }

    @Test
    void dirtHeapsEarthAndHurtsNobody() {
        Battle battle = Battles.battle(Theme.DESERT, 5, Control.PERSON, Control.PERSON);
        Tank target = battle.tanks().get(1);
        battle.current().select(Weapon.DIRT);
        double x = (battle.current().x() + target.x()) / 2;
        double surface = battle.ground().surface(x);
        List<Event> events = shootAt(battle, x, surface, 3);
        assertTrue(events.stream().anyMatch(e -> e instanceof Event.Heaped));
        assertTrue(battle.ground().surface(x) < surface - 10, "a wall of earth: " + surface + " -> " + battle.ground().surface(x));
        assertEquals(Field.HEALTH, target.health());
        assertEquals(1, battle.tanks().getFirst().stock(Weapon.DIRT));
    }

    @Test
    void aBarrelBuriedInEarthFiresIntoIt() {
        Battle battle = Battles.battle(Theme.DESERT, 6, Control.PERSON, Control.PERSON);
        Tank tank = battle.current();
        battle.ground().heap(tank.x(), tank.y() - 10, 30, Earth.DIRT);
        battle.update(STEP, Input.firing());
        assertTrue(battle.events().stream().anyMatch(e -> e instanceof Event.Exploded), "at once, at the muzzle");
        assertTrue(tank.health() < Field.HEALTH, "and it hurts");
    }

    @Test
    void weaponsRunOutAndThePlainShellIsLoadedAgain() {
        Battle battle = Battles.battle(Theme.DESERT, 7, Control.PERSON, Control.PERSON);
        Tank first = battle.current();
        for (int shot = 0; shot < 2; shot++) {
            while (battle.current() != first) {
                battle.update(STEP, Input.aim(90, 5, true));
                settle(battle);
            }
            first.select(Weapon.HEAVY);
            battle.update(STEP, Input.aim(first.x() < 320 ? 30 : 150, 20, true));
            settle(battle);
        }
        assertEquals(0, first.stock(Weapon.HEAVY));
        assertEquals(Weapon.SHELL, first.weapon());
        first.cycleWeapon(1);
        assertEquals(Weapon.TRIPLE, first.weapon(), "the heavy shell is skipped now");
        assertEquals(-1, first.stock(Weapon.SHELL));
    }

    // --8<-- [start:test-wind]
    @Test
    void theWindChangesALittleEachTurnAndTheMoonHasNone() {
        Battle battle = Battles.battle(Theme.VALLEY, 8, Control.PERSON, Control.PERSON);
        int wind = battle.wind();
        for (int turn = 0; turn < 12; turn++) {
            battle.update(STEP, Input.aim(battle.current().x() < 320 ? 30 : 150, 15, true));
            settle(battle);
            assertTrue(Math.abs(battle.wind() - wind) <= Field.WIND_CHANGE);
            assertTrue(Math.abs(battle.wind()) <= Field.MAX_WIND);
            wind = battle.wind();
        }
        Battle moon = Battles.battle(Theme.MOON, 8, Control.PERSON, Control.PERSON);
        assertEquals(0, moon.wind());
        assertEquals(Field.GRAVITY * 0.4, moon.gravity(), 1e-9);
    }
    // --8<-- [end:test-wind]

    // --8<-- [start:test-replay]
    @Test
    void theRoundEndsWithOneTankLeftAndTheDecisiveShotPlaysAgainTheSame() {
        Battle battle = Battles.battle(Theme.WINTER, 9, Control.HARD, Control.HARD);
        Battles.runUntil(battle, new Computer(new SeededRandom(9)), 300, b -> b.phase() == Battle.Phase.OVER);
        assertEquals(Battle.Phase.OVER, battle.phase());
        Tank winner = battle.winner();
        assertNotNull(winner);
        Tank loser = battle.tanks().stream().filter(t -> t != winner).findFirst().orElseThrow();

        Battle replay = battle.replay();
        assertNotNull(replay);
        replay.update(STEP, battle.replayShot());
        Battles.idle(replay, 15);
        assertEquals(Battle.Phase.OVER, replay.phase());
        assertEquals(winner.seat(), replay.winner().seat());
        assertEquals(winner.health(), replay.winner().health(), "the same shot, the same damage");
        assertEquals(loser.y(), replay.tanks().get(battle.tanks().indexOf(loser)).y(), 1e-9);
    }
    // --8<-- [end:test-replay]
}
