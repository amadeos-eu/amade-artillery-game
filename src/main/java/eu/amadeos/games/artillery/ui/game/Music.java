package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.gameengine.audio.Sound;
import eu.amadeos.gameengine.audio.Sound.Wave;
import eu.amadeos.gameengine.audio.Tune;

/**
 * The tunes of Artillery, in MML: a march of drums and a bugle at the start of a round, a short fanfare for
 * the round's winner, a longer one for the match, and a sad one when nobody is left.
 */
final class Music {
    final Sound round = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.06, "t140 o4 l8 g g16 g16 >c4 < g8. g16 >c e4 c2"),
            Tune.voice(Wave.NOISE, 0.08, "t140 o4 l16 c c c8 c8 c c c8 c8 c c c c c4 r4"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t140 o2 l4 c g c g c2"));

    final Sound roundWon = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.07, "t170 o5 l8 c e g >c4 < g >c2"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t170 o3 l8 c g >c4 < g c2"));

    final Sound matchWon = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.07, "t150 o5 l8 g g g e4. g16 >c2 < a8 a8 a8 f4. a16 >d2 c4 < b4 >c2"),
            Tune.voice(Wave.TRIANGLE, 0.22, "t150 o3 l4 c g c g f >c < f >c < g b >c2"));

    final Sound nobody = Sound.mix(
            Tune.voice(Wave.SQUARE, 0.06, "t100 o4 l4 e d c < b | a2. r4"),
            Tune.voice(Wave.TRIANGLE, 0.2, "t100 o2 l2 a e | a1"));
}
