package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;

/** The sounds of Artillery, made once at start from tones: no recordings. */
final class SoundBank {
    /** The boom of a shot: a thump and a crack of noise. */
    final Sound fire = Sound.mix(Sound.tone(Wave.TRIANGLE, 110, 40, 0.3, 0.45), Sound.tone(Wave.NOISE, 0, 0, 0.18, 0.2));
    final Sound heavyFire = Sound.mix(Sound.tone(Wave.TRIANGLE, 80, 30, 0.45, 0.5), Sound.tone(Wave.NOISE, 0, 0, 0.3, 0.25));
    final Sound tripleFire = Sound.tone(Wave.NOISE, 0, 0, 0.06, 0.18).then(Sound.tone(Wave.NOISE, 0, 0, 0.06, 0.15))
            .then(Sound.tone(Wave.NOISE, 0, 0, 0.06, 0.12));
    final Sound dirtFire = Sound.mix(Sound.tone(Wave.TRIANGLE, 150, 70, 0.2, 0.3), Sound.tone(Wave.NOISE, 0, 0, 0.1, 0.08));
    final Sound explosion = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.7, 0.3), Sound.tone(Wave.TRIANGLE, 90, 25, 0.6, 0.4));
    final Sound bigExplosion = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 1.2, 0.35), Sound.tone(Wave.TRIANGLE, 70, 20, 1.0, 0.5));
    /** Earth landing in a heap: a soft rush. */
    final Sound heap = Sound.mix(Sound.tone(Wave.NOISE, 0, 0, 0.5, 0.12), Sound.tone(Wave.TRIANGLE, 60, 40, 0.3, 0.2));
    /** Metal hit. */
    final Sound clank = Sound.mix(Sound.tone(Wave.SQUARE, 330, 180, 0.12, 0.08), Sound.tone(Wave.NOISE, 0, 0, 0.08, 0.1));
    final Sound thud = Sound.tone(Wave.TRIANGLE, 120, 50, 0.18, 0.35);
    /** A soft click every five degrees the barrel turns, and when a weapon is loaded. */
    final Sound tick = Sound.tone(Wave.SQUARE, 1200, 1200, 0.012, 0.03);
    final Sound load = Sound.tone(Wave.SQUARE, 440, 660, 0.06, 0.05).then(Sound.tone(Wave.SQUARE, 880, 880, 0.04, 0.04));
    final Sound turn = Sound.tone(Wave.TRIANGLE, 660, 990, 0.12, 0.12);

    /** The whistle of a falling shell, from the top of its arc down to the ground: high to low. */
    static Sound whistle(double seconds) {
        return Sound.tone(Wave.TRIANGLE, 1500, 520, Math.max(0.3, Math.min(2.0, seconds)), 0.045);
    }
}
