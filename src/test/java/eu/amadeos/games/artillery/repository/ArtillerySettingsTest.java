package eu.amadeos.games.artillery.repository;

import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtillerySettingsTest {
    @TempDir
    Path dir;

    @Test
    void settingsAreStoredAndReadBack() {
        ArtillerySettings settings = new ArtillerySettings(dir.resolve("artillery.properties"));
        assertEquals(RenderMode.DIRECT, settings.renderMode());
        assertTrue(settings.sound());
        settings.setRenderMode(RenderMode.PIXEL);
        settings.setSound(false);
        ArtillerySettings again = new ArtillerySettings(dir.resolve("artillery.properties"));
        assertEquals(RenderMode.PIXEL, again.renderMode());
        assertFalse(again.sound());
    }

    @Test
    void theLastSetupIsKept() {
        ArtillerySettings settings = new ArtillerySettings(dir.resolve("artillery.properties"));
        assertEquals(Match.Setup.standard(), settings.setup());
        Match.Setup setup = new Match.Setup(List.of(new Seat(0, Control.PERSON), new Seat(1, Control.PERSON),
                new Seat(2, Control.HARD)), 3, Theme.WINTER);
        settings.setSetup(setup);
        assertEquals(setup, new ArtillerySettings(dir.resolve("artillery.properties")).setup());
    }

    @Test
    void aBrokenSetupFallsBackToTheStandardOne() throws Exception {
        Path file = dir.resolve("artillery.properties");
        Files.writeString(file, "match.players=person\nmatch.rounds=2\n");
        assertEquals(Match.Setup.standard(), new ArtillerySettings(file).setup());
        Files.writeString(file, "match.players=person robot\n");
        assertEquals(Match.Setup.standard(), new ArtillerySettings(file).setup());
        Files.writeString(file, "match.players=person hard\nmatch.landscape=mars\n");
        assertEquals(Match.Setup.standard(), new ArtillerySettings(file).setup());
    }

    @Test
    void keysComeFromTheFileOrTheDefaults() throws Exception {
        Path file = dir.resolve("artillery.properties");
        Files.writeString(file, "keys.fire=F CONTROL\nkeys.left=NOT_A_KEY\n");
        ArtillerySettings settings = new ArtillerySettings(file);
        assertArrayEquals(new int[]{KeyEvent.VK_F, KeyEvent.VK_CONTROL}, settings.keys("fire", KeyEvent.VK_SPACE));
        assertArrayEquals(new int[]{KeyEvent.VK_LEFT}, settings.keys("left", KeyEvent.VK_LEFT));
    }
}
