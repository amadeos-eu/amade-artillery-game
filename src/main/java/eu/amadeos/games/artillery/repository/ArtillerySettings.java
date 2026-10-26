package eu.amadeos.games.artillery.repository;

import com.google.inject.Inject;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;
import eu.amadeos.gameengine.swing.RenderMode;
import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Theme;

import javax.swing.KeyStroke;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * The settings in {@code ~/.config/amadeos/artillery/artillery.properties}: the graphics mode, the sound,
 * the keys and the last match's setup (who plays, rounds to win, landscape). Keys are written as Java names
 * them, several separated by spaces, e.g. {@code keys.fire=SPACE CONTROL}.
 */
public final class ArtillerySettings {
    private static final String GRAPHICS = "graphics";
    private static final String SOUND = "sound";
    private static final String PLAYERS = "match.players";
    private static final String TO_WIN = "match.rounds";
    private static final String LANDSCAPE = "match.landscape";
    private static final String CHANGING = "changing";

    private final Path file;

    @Inject
    public ArtillerySettings(AppDirectories directories) {
        this(directories.configFile("artillery.properties"));
    }

    public ArtillerySettings(Path file) {
        this.file = file;
    }

    public RenderMode renderMode() {
        return RenderMode.ofId(PropertiesFile.load(file).getProperty(GRAPHICS, "")).orElse(RenderMode.DIRECT);
    }

    public void setRenderMode(RenderMode mode) {
        set(GRAPHICS, mode.id());
    }

    public boolean sound() {
        return !"off".equals(PropertiesFile.load(file).getProperty(SOUND, "on"));
    }

    public void setSound(boolean on) {
        set(SOUND, on ? "on" : "off");
    }

    /** The setup of the last match, or the standard one: a person against the computer. */
    public Match.Setup setup() {
        Properties p = PropertiesFile.load(file);
        try {
            List<Seat> seats = new ArrayList<>();
            String[] controls = p.getProperty(PLAYERS, "").strip().split("\\s+");
            for (int i = 0; i < controls.length && i < Seat.MAX; i++) {
                seats.add(new Seat(i, Control.valueOf(controls[i].toUpperCase(Locale.ROOT))));
            }
            int toWin = Integer.parseInt(p.getProperty(TO_WIN, "2"));
            String landscape = p.getProperty(LANDSCAPE, CHANGING);
            Theme theme = landscape.equals(CHANGING) ? null : Theme.valueOf(landscape.toUpperCase(Locale.ROOT));
            return new Match.Setup(seats, Math.min(5, toWin), theme);
        } catch (IllegalArgumentException e) {
            return Match.Setup.standard();
        }
    }

    public void setSetup(Match.Setup setup) {
        Properties p = PropertiesFile.load(file);
        p.setProperty(PLAYERS, String.join(" ", setup.seats().stream().map(s -> s.control().id()).toList()));
        p.setProperty(TO_WIN, Integer.toString(setup.toWin()));
        p.setProperty(LANDSCAPE, setup.theme() == null ? CHANGING : setup.theme().id());
        PropertiesFile.store(file, p, "amade-artillery-game settings");
    }

    private void set(String key, String value) {
        Properties properties = PropertiesFile.load(file);
        properties.setProperty(key, value);
        PropertiesFile.store(file, properties, "amade-artillery-game settings");
    }

    /**
     * The keys of an action, e.g. {@code keys("fire", KeyEvent.VK_SPACE)}: those in the file if it names any it
     * understands, else the defaults.
     */
    public int[] keys(String action, int... defaults) {
        String names = PropertiesFile.load(file).getProperty("keys." + action, "").strip();
        List<Integer> codes = new ArrayList<>();
        for (String name : names.split("\\s+")) {
            if (name.isEmpty()) continue;
            KeyStroke stroke = KeyStroke.getKeyStroke(name.toUpperCase(Locale.ROOT));
            if (stroke != null && stroke.getKeyCode() != 0) codes.add(stroke.getKeyCode());
        }
        return codes.isEmpty() ? defaults : codes.stream().mapToInt(Integer::intValue).toArray();
    }
}
