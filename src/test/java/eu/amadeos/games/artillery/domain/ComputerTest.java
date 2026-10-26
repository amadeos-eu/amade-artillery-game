package eu.amadeos.games.artillery.domain;

import eu.amadeos.gameengine.ai.ShotSolver;
import eu.amadeos.gameengine.random.SeededRandom;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComputerTest {
    // --8<-- [start:test-computer]
    @Test
    void thereIsAlmostAlwaysAShotThatHitsAndTheSearchFindsIt() {
        int found = 0, tries = 0;
        for (Theme theme : Theme.values()) {
            for (long seed = 1; seed <= 5; seed++) {
                Battle battle = Battles.battle(theme, seed, Control.HARD, Control.HARD);
                Tank tank = battle.current(), target = battle.tanks().get(1);
                ShotSolver.Aim aim = Computer.aim(battle, tank, target, battle.air());
                tries++;
                if (aim.miss() < Field.TANK_HEIGHT / 2) found++;
            }
        }
        assertTrue(found >= tries - 1, found + " of " + tries + " landscapes");
    }

    @Test
    void theLowerTheLevelTheBiggerTheMistakes() {
        double[] error = new double[3];
        Control[] levels = {Control.EASY, Control.NORMAL, Control.HARD};
        for (int l = 0; l < 3; l++) {
            for (long seed = 1; seed <= 8; seed++) {
                Battle battle = Battles.battle(Theme.DESERT, seed, levels[l], levels[l]);
                Computer computer = new Computer(new SeededRandom(seed));
                Computer.Plan plan = computer.plan(battle);
                ShotSolver.Aim best = Computer.aim(battle, battle.current(), plan.target(), battle.air());
                error[l] += Math.abs(plan.angle() - best.angle()) + Math.abs(plan.power() - best.power());
            }
        }
        assertTrue(error[0] > error[1] && error[1] > error[2], java.util.Arrays.toString(error));
    }
    // --8<-- [end:test-computer]

    @Test
    void itTurnsTheBarrelLikeAPlayerAndThenFires() {
        Battle battle = Battles.battle(Theme.VALLEY, 2, Control.NORMAL, Control.NORMAL);
        Computer computer = new Computer(new SeededRandom(2));
        Tank tank = battle.current();
        double start = tank.angle();
        Input first = computer.steer(battle, Battles.STEP);
        assertEquals(Input.NONE, first, "it thinks first");
        double seconds = Battles.runUntil(battle, computer, 10, b -> b.phase() != Battle.Phase.AIM);
        assertTrue(seconds > Computer.THINK_SECONDS + Computer.STEADY_SECONDS, "not at once: " + seconds);
        assertTrue(seconds < 5, "but within a few seconds: " + seconds);
        assertTrue(tank.angle() != start || tank.power() != 60);
    }

    // --8<-- [start:test-everywhere]
    @Test
    void computersFinishARoundOnEveryLandscape() {
        for (Theme theme : Theme.values()) {
            for (long seed = 1; seed <= 2; seed++) {
                Battle battle = Battles.battle(theme, seed, Control.NORMAL, Control.HARD, Control.EASY);
                Battles.runUntil(battle, new Computer(new SeededRandom(seed)), 900, b -> b.phase() == Battle.Phase.OVER);
                assertEquals(Battle.Phase.OVER, battle.phase(), theme + " " + seed + ": " + battle.tanks());
            }
        }
    }
    // --8<-- [end:test-everywhere]
}
