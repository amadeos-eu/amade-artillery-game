package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.games.artillery.domain.Battle;
import eu.amadeos.games.artillery.domain.Control;
import eu.amadeos.games.artillery.domain.Field;
import eu.amadeos.games.artillery.domain.Match;
import eu.amadeos.games.artillery.domain.Seat;
import eu.amadeos.games.artillery.domain.Tank;
import eu.amadeos.games.artillery.domain.Theme;
import eu.amadeos.games.artillery.domain.Weapon;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The overlay: the top bar with the tank whose turn it is, its angle, power and weapon, the wind and the
 * rounds won; the names and health over the tanks and the damage rising from them; the banners; and the
 * cards of the title, the setup, the pause, the end of a round and of the match. Drawn after the world, at
 * the screen's full size, so the letters are sharp in every render mode.
 */
final class Hud {
    static final Color CHALK = Color.WHITE;
    static final Color GOLD = new Color(0xFFE27A);
    static final Color GREY = new Color(0x9AA0B8);
    static final Color RED = new Color(0xFF5A3A);
    static final Color BAR_TOP = new Color(0x262A3C);
    static final Color BAR_BOTTOM = new Color(0x161924);
    static final Color FIELD = new Color(0x2C3248);
    static final Color VEIL = new Color(0, 0, 0, 150);
    static final Color SHADOW = new Color(0, 0, 0, 140);
    static final double CARD_ROW = 22;

    private final Messages msg;
    private final Map<Float, Font> plain = new HashMap<>();
    private final Map<Float, Font> bold = new HashMap<>();

    Hud(Messages msg) {
        this.msg = msg;
    }

    String name(Seat seat) {
        return msg.get("seat." + seat.number());
    }

    // --8<-- [start:status]
    /** The bar along the top. */
    void status(Graphics2D g, Battle battle, Match match) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, BAR_TOP, 0, (float) Field.TOP, BAR_BOTTOM));
        g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.TOP));
        g.setColor(new Color(0x3A4058));
        g.fill(new Rectangle2D.Double(0, Field.TOP - 1, Field.WIDTH, 1));

        Tank tank = battle.current();
        g.setFont(font(9, true));
        g.setColor(Look.seat(tank.seat().number()));
        g.drawString(name(tank.seat()), 8, 15.5f);
        g.setFont(font(9, false));
        g.setColor(CHALK);
        g.drawString(msg.format("hud.angle", Math.round(tank.angle())), 50, 15.5f);
        g.drawString(msg.format("hud.power", Math.round(tank.power())), 108, 15.5f);

        // The weapon, with arrows: Tab and Q switch it.
        Weapon w = tank.weapon();
        g.setColor(FIELD);
        g.fill(new RoundRectangle2D.Double(160, 5, 110, 14, 4, 4));
        g.setColor(GOLD);
        g.fill(new Polygon(new int[]{165, 169, 169}, new int[]{12, 9, 15}, 3));
        g.fill(new Polygon(new int[]{265, 261, 261}, new int[]{12, 9, 15}, 3));
        String label = msg.get("weapon." + w.id()) + (tank.stock(w) > 0 ? "  ×" + tank.stock(w) : "");
        g.setFont(font(8.5f, false));
        g.drawString(label, (float) (215 - g.getFontMetrics().stringWidth(label) / 2.0), 15.5f);

        wind(g, battle);

        g.setFont(font(9, false));
        g.setColor(CHALK);
        String round = msg.format("hud.round", match.round());
        double x = Field.WIDTH - 8 - 34 * match.setup().seats().size();
        g.drawString(round, (float) (x - 8 - g.getFontMetrics().stringWidth(round)), 15.5f);
        for (Seat s : match.setup().seats()) {
            g.setColor(Look.seat(s.number()));
            g.fill(new Ellipse2D.Double(x, 8, 8, 8));
            g.setColor(CHALK);
            g.drawString(Integer.toString(match.wins(s)), (float) x + 12, 15.5f);
            x += 34;
        }
    }

    /** The wind: an arrow growing from the middle of a bar, to the side it blows to, and its strength. */
    private void wind(Graphics2D g, Battle battle) {
        double left = 330, width = 80, middle = left + width / 2;
        g.setFont(font(8.5f, false));
        g.setColor(GREY);
        String label = msg.get("hud.wind");
        g.drawString(label, (float) (left - 6 - g.getFontMetrics().stringWidth(label)), 15.5f);
        g.setColor(FIELD);
        g.fill(new RoundRectangle2D.Double(left, 8, width, 8, 4, 4));
        int wind = battle.wind();
        g.setColor(new Color(0x8AD0FF));
        if (wind != 0) {
            double length = width / 2 * Math.abs(wind) / Field.MAX_WIND;
            double tip = middle + Math.signum(wind) * length;
            g.fill(new Rectangle2D.Double(Math.min(middle, tip - Math.signum(wind) * 4), 9.5,
                    Math.max(0, length - 4), 5));
            int t = (int) Math.round(tip), d = wind > 0 ? -6 : 6;
            g.fill(new Polygon(new int[]{t + d, t, t + d}, new int[]{7, 12, 17}, 3));
        }
        g.setColor(new Color(0x6A7088));
        g.fill(new Rectangle2D.Double(middle, 6, 1, 12));
        g.setColor(CHALK);
        String value = !battle.theme().windy() ? msg.get("hud.none") : wind == 0 ? msg.get("hud.calm")
                : Math.abs(wind) + (wind > 0 ? " →" : " ←");
        g.drawString(value, (float) (left + width + 6), 15.5f);
    }
    // --8<-- [end:status]

    /** The names and health bars over the tanks, and the damage rising from those hit. */
    void labels(Graphics2D g, Battle battle, Effects effects, double alpha) {
        for (Tank t : battle.tanks()) {
            if (!t.alive()) continue;
            double x = t.x(), y = t.y(alpha) - 30;
            if (t == battle.current() && battle.phase() == Battle.Phase.AIM) {
                double bob = Math.abs(Math.sin(effects.time() * 4)) * 3;
                g.setColor(GOLD);
                g.fill(new Polygon(new int[]{(int) x - 4, (int) x + 4, (int) x}, new int[]{(int) (y - 16 - bob),
                        (int) (y - 16 - bob), (int) (y - 10 - bob)}, 3));
            }
            g.setFont(font(7.5f, true));
            String name = name(t.seat());
            double w = Math.max(32, g.getFontMetrics().stringWidth(name) + (t.seat().control().computer() ? 16 : 8));
            g.setColor(new Color(0, 0, 0, 110));
            g.fill(new RoundRectangle2D.Double(x - w / 2, y - 9, w, 14, 4, 4));
            double textX = x - g.getFontMetrics().stringWidth(name) / 2.0;
            if (t.seat().control().computer()) {
                textX += 4;
                screen(g, textX - 9, y - 6.5);
            }
            g.setColor(CHALK);
            g.drawString(name, (float) textX, (float) y - 1);
            g.setColor(new Color(0x303030));
            g.fill(new Rectangle2D.Double(x - 14, y + 1.5, 28, 2.4));
            g.setColor(t.health() > 60 ? new Color(0x7AD04A) : t.health() > 30 ? new Color(0xE0B03A) : RED);
            g.fill(new Rectangle2D.Double(x - 14, y + 1.5, 28 * t.health() / (double) Field.HEALTH, 2.4));
        }
        for (Effects.Damage d : effects.damage()) {
            double age = effects.time() - d.born();
            int a = (int) (255 * Math.clamp((Effects.DAMAGE_SECONDS - age) / 0.5, 0, 1));
            g.setFont(font(10, true));
            String text = "−" + d.amount();
            float tx = (float) d.x() - g.getFontMetrics().stringWidth(text) / 2f, ty = (float) (d.y() - 26 - age * 18);
            g.setColor(new Color(0, 0, 0, a / 2));
            g.drawString(text, tx + 1, ty + 1);
            g.setColor(new Color(RED.getRed(), RED.getGreen(), RED.getBlue(), a));
            g.drawString(text, tx, ty);
        }
    }

    /** A small screen: the sign of a tank the computer drives. */
    private static void screen(Graphics2D g, double x, double y) {
        g.setColor(GREY);
        g.fill(new RoundRectangle2D.Double(x, y, 7, 5, 1.5, 1.5));
        g.fill(new Rectangle2D.Double(x + 2.5, y + 5, 2, 1.2));
        g.setColor(new Color(0x8AD0FF));
        g.fill(new Rectangle2D.Double(x + 1, y + 1, 5, 3));
    }

    /** At the start of a round its number and landscape; at each turn whose it is. */
    void banners(Graphics2D g, Battle battle, Match match, Effects effects) {
        if (battle.time() < Effects.BANNER_SECONDS) {
            fading(g, msg.format("play.round", match.round(), msg.get("theme." + battle.theme().id())), 120, 20, GOLD,
                    battle.time(), Effects.BANNER_SECONDS);
        } else if (effects.turn() != null && effects.sinceTurn() < Effects.BANNER_SECONDS * 0.7
                && battle.phase() == Battle.Phase.AIM) {
            Tank tank = effects.turn();
            fading(g, msg.format("play.turn", name(tank.seat())), 120, 15, Look.seat(tank.seat().number()),
                    effects.sinceTurn(), Effects.BANNER_SECONDS * 0.7);
        }
    }

    private void fading(Graphics2D g, String line, double y, float size, Color colour, double shown, double seconds) {
        int alpha = (int) Math.round(255 * Math.clamp((seconds - shown) / 0.5, 0, 1));
        g.setFont(font(size, true));
        double x = (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2;
        g.setColor(new Color(0, 0, 0, alpha / 2));
        g.drawString(line, (float) x + 1.5f, (float) y + 1.5f);
        g.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha));
        g.drawString(line, (float) x, (float) y);
    }

    // ---- the screens around the battle -------------------------------------------------------------------

    void title(Graphics2D g, double time) {
        veil(g);
        g.setFont(font(54, true));
        String title = msg.get("window.title");
        double x = (Field.WIDTH - g.getFontMetrics().stringWidth(title)) / 2;
        g.setColor(new Color(0, 0, 0, 120));
        g.drawString(title, (float) x + 3, 133);
        g.setColor(GOLD);
        g.drawString(title, (float) x, 130);
        // the four colours of the tanks under the name
        for (int i = 0; i < 4; i++) {
            g.setColor(Look.seat(i));
            g.fill(new Rectangle2D.Double(Field.WIDTH / 2 - 120 + i * 60, 142, 60, 4));
        }
        text(g, msg.get("title.tagline"), 172, 12, false, CHALK);
        if ((time * 1.5) % 1 < 0.7) text(g, msg.get("title.start"), 240, 16, true, CHALK);
        text(g, msg.get("title.keys"), 290, 10, false, CHALK);
        text(g, msg.get("title.mouse"), 306, 10, false, CHALK);
        text(g, msg.get("title.screen"), 322, 10, false, GREY);
    }

    // --8<-- [start:setup]
    /** The card before a match. */
    void setup(Graphics2D g, SetupCard card) {
        veil(g);
        List<SetupCard.Line> lines = card.lines();
        double top = cardTop(lines.size());
        text(g, msg.get("setup.title"), top - 18, 20, true, GOLD);
        for (int i = 0; i < lines.size(); i++) {
            SetupCard.Line line = lines.get(i);
            double y = top + i * CARD_ROW;
            boolean selected = i == card.selected();
            g.setColor(selected ? new Color(255, 255, 255, 40) : new Color(0, 0, 0, 90));
            g.fill(new RoundRectangle2D.Double(150, y, 340, CARD_ROW - 3, 6, 6));
            if (line.row() == SetupCard.Row.START) {
                text(g, msg.get("setup.start"), y + 13.5, 11, true, selected ? GOLD : CHALK);
                continue;
            }
            String label = switch (line.row()) {
                case PLAYERS -> msg.get("setup.players");
                case SEAT -> name(new Seat(line.seat(), Control.PERSON));
                case ROUNDS -> msg.get("setup.rounds");
                default -> msg.get("setup.landscape");
            };
            String value = switch (line.row()) {
                case PLAYERS -> Integer.toString(card.players());
                case SEAT -> msg.get("control." + card.control(line.seat()).id());
                case ROUNDS -> Integer.toString(card.toWin());
                default -> card.theme() == null ? msg.get("theme.changing") : msg.get("theme." + card.theme().id());
            };
            g.setFont(font(10, line.row() == SetupCard.Row.SEAT));
            g.setColor(line.row() == SetupCard.Row.SEAT ? Look.seat(line.seat()) : GREY);
            g.drawString(label, 162, (float) (y + 13.5));
            g.setFont(font(10, false));
            g.setColor(selected ? GOLD : CHALK);
            int vw = g.getFontMetrics().stringWidth(value);
            g.drawString(value, (float) (400 - vw / 2.0), (float) (y + 13.5));
            if (selected) {
                g.fill(new Polygon(new int[]{330, 335, 335}, new int[]{(int) y + 9, (int) y + 6, (int) y + 12}, 3));
                g.fill(new Polygon(new int[]{475, 470, 470}, new int[]{(int) y + 9, (int) y + 6, (int) y + 12}, 3));
            }
        }
        text(g, msg.get("setup.hint"), top + lines.size() * CARD_ROW + 18, 9, false, GREY);
    }
    // --8<-- [end:setup]

    /** Where the card's first row is, so it is centred on the screen. */
    static double cardTop(int rows) {
        return (Field.HEIGHT - rows * CARD_ROW) / 2 + 10;
    }

    /** The row of the setup card under a point, or -1. */
    static int setupRowAt(SetupCard card, double x, double y) {
        if (x < 150 || x > 490) return -1;
        double top = cardTop(card.lines().size());
        int row = (int) Math.floor((y - top) / CARD_ROW);
        return row >= 0 && row < card.lines().size() && y - top - row * CARD_ROW < CARD_ROW - 3 ? row : -1;
    }

    void pause(Graphics2D g) {
        veil(g);
        text(g, msg.get("pause.title"), 190, 36, true, CHALK);
        text(g, msg.get("pause.continue"), 230, 13, false, CHALK);
        text(g, msg.get("pause.leave"), 250, 11, false, GREY);
    }

    void replay(Graphics2D g, double time) {
        g.setFont(font(13, true));
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new RoundRectangle2D.Double(10, Field.TOP + 10, 120, 22, 6, 6));
        if ((time * 2) % 1 < 0.6) {
            g.setColor(RED);
            g.fill(new Ellipse2D.Double(18, Field.TOP + 17, 8, 8));
        }
        g.setColor(CHALK);
        g.drawString(msg.get("replay.title"), 32, (float) Field.TOP + 26);
        g.setFont(font(9, false));
        g.setColor(GREY);
        g.drawString(msg.get("replay.skip"), 10, (float) Field.HEIGHT - 10);
    }

    // --8<-- [start:cards]
    /** The end of a round: who won it, and the table of rounds won and points. */
    void roundOver(Graphics2D g, Match match, Tank winner) {
        veil(g);
        if (winner == null) text(g, msg.get("round.nobody"), 110, 24, true, CHALK);
        else text(g, msg.format("round.won", name(winner.seat()), match.round()), 110, 24, true,
                Look.seat(winner.seat().number()));
        table(g, match, 150);
        text(g, msg.get(match.over() ? "round.last" : "round.next"), 330, 12, false, CHALK);
    }

    void matchOver(Graphics2D g, Match match) {
        veil(g);
        Match.Result result = match.result();
        if (result.winner() == null) text(g, msg.get("match.draw"), 110, 26, true, CHALK);
        else text(g, msg.format("match.won", name(result.winner())), 110, 26, true, Look.seat(result.winner().number()));
        table(g, match, 150);
        text(g, msg.get("match.continue"), 330, 12, false, GREY);
    }

    private void table(Graphics2D g, Match match, double top) {
        List<Seat> standings = match.standings();
        for (int i = 0; i < standings.size(); i++) {
            Seat s = standings.get(i);
            double y = top + i * 26;
            g.setColor(new Color(0, 0, 0, 90));
            g.fill(new RoundRectangle2D.Double(170, y, 300, 22, 6, 6));
            g.setColor(Look.seat(s.number()));
            g.fill(new Ellipse2D.Double(180, y + 7, 8, 8));
            g.setFont(font(11, true));
            g.drawString(name(s), 196, (float) y + 15);
            g.setFont(font(9, false));
            g.setColor(GREY);
            g.drawString(msg.get("control." + s.control().id()), 250, (float) y + 15);
            for (int w = 0; w < match.setup().toWin(); w++) {
                g.setColor(w < match.wins(s) ? GOLD : new Color(0x4A4E60));
                g.fill(new Ellipse2D.Double(375 + w * 9, y + 8, 6, 6));
            }
            g.setColor(CHALK);
            g.setFont(font(10, true));
            String points = msg.format("round.points", match.points(s));
            g.drawString(points, (float) (462 - g.getFontMetrics().stringWidth(points)), (float) y + 15);
        }
    }
    // --8<-- [end:cards]

    void veil(Graphics2D g) {
        g.setColor(VEIL);
        g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT));
    }

    /** A line of text centred across the screen, its baseline at {@code y}. */
    void text(Graphics2D g, String line, double y, float size, boolean strong, Color colour) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font(size, strong));
        double x = (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2;
        g.setColor(SHADOW);
        g.drawString(line, (float) x + 1, (float) y + 1);
        g.setColor(colour);
        g.drawString(line, (float) x, (float) y);
    }

    Font font(float size, boolean strong) {
        return (strong ? bold : plain).computeIfAbsent(size,
                s -> new Font(Font.SANS_SERIF, strong ? Font.BOLD : Font.PLAIN, 1).deriveFont(s));
    }

    /** The name of a landscape in the player's language. */
    String theme(Theme theme) {
        return msg.get("theme." + theme.id());
    }
}
