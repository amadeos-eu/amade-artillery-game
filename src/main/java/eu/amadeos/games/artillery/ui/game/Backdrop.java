package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.games.artillery.domain.Field;
import eu.amadeos.games.artillery.domain.Theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;
import java.util.Map;

/**
 * The sky and the far landscape behind the battlefield, one picture per theme, drawn once and copied in
 * every frame: an alpine valley at sunset, dunes under a white sun, winter peaks at night, the moon with
 * the earth in its black sky. Nothing in it moves, and shells fly in front of it.
 */
final class Backdrop {
    private final Map<Theme, Sprite> pictures = new EnumMap<>(Theme.class);

    void draw(Graphics2D g, Theme theme) {
        pictures.computeIfAbsent(theme, t -> Sprite.drawn(Field.WIDTH, Field.HEIGHT, (sg, w, h) -> paint(sg, t)).opaque())
                .draw(g, 0, 0);
    }

    // --8<-- [start:paint]
    static void paint(Graphics2D g, Theme theme) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        SeededRandom r = new SeededRandom(theme.ordinal() + 11);
        switch (theme) {
            case VALLEY -> {
                sky(g, new float[]{0f, 0.5f, 0.85f, 1f}, 0x2A3C78, 0x6B6AA8, 0xE89A78, 0xF6C88A);
                glow(g, 380, 250, 120, 0xFFF0C0, 150);
                disc(g, 366, 236, 28, 0xFFF3D0);
                clouds(g, r, 0xFDE6E0, 150);
                ridge(g, r, 210, 40, 0x8A7FB0, 0xE8E4F4);
                ridge(g, r, 240, 30, 0x6F6A98, 0);
            }
            case DESERT -> {
                sky(g, new float[]{0f, 0.55f, 1f}, 0x3B8FD8, 0x9FD0F0, 0xF4E2B8);
                glow(g, 500, 70, 80, 0xFFFFF0, 160);
                disc(g, 486, 56, 28, 0xFFFFF4);
                mesas(g, r);
            }
            case WINTER -> {
                sky(g, new float[]{0f, 0.6f, 1f}, 0x0B1030, 0x23305A, 0x4A5A8A);
                stars(g, r, 120, 230);
                aurora(g);
                disc(g, 140, 70, 22, 0xF4F0E0);
                g.setColor(new Color(0x0F1638));
                g.fill(new Ellipse2D.Double(147, 66, 20, 20));
                ridge(g, r, 200, 55, 0x3A4A78, 0xC8D4F0);
                ridge(g, r, 235, 35, 0x2C3862, 0x9AAAD0);
            }
            case MOON -> {
                g.setColor(new Color(0x05060A));
                g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT));
                stars(g, r, 220, Field.HEIGHT);
                earth(g, 520, 80, 26);
                ridge(g, r, 250, 30, 0x34343A, 0);
            }
        }
    }
    // --8<-- [end:paint]

    private static void sky(Graphics2D g, float[] stops, int... colours) {
        Color[] c = new Color[colours.length];
        for (int i = 0; i < colours.length; i++) c[i] = new Color(colours[i]);
        g.setPaint(new LinearGradientPaint(0, 0, 0, (float) Field.HEIGHT, stops, c));
        g.fill(new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT));
    }

    private static void glow(Graphics2D g, double x, double y, double radius, int rgb, int alpha) {
        Color inner = new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha);
        Color outer = new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 0);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) radius, new float[]{0f, 1f}, new Color[]{inner, outer}));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }

    private static void disc(Graphics2D g, double x, double y, double size, int rgb) {
        g.setColor(new Color(rgb));
        g.fill(new Ellipse2D.Double(x, y, size, size));
    }

    private static void clouds(Graphics2D g, SeededRandom r, int rgb, int alpha) {
        g.setColor(new Color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, alpha));
        for (int c = 0; c < 4; c++) {
            double x = 60 + c * 150 + r.nextDouble() * 60, y = 50 + r.nextDouble() * 60, s = 0.6 + r.nextDouble() * 0.5;
            for (int i = 0; i < 5; i++) {
                double w = (30 + i * 7) * s;
                g.fill(new Ellipse2D.Double(x + (i - 2) * 16 * s - w / 2, y - (i % 2) * 6 * s, w, 16 * s));
            }
        }
    }

    /** A far mountain ridge, with snow on its tops if {@code snow} is not 0. */
    private static void ridge(Graphics2D g, SeededRandom r, double base, double height, int rgb, int snow) {
        Path2D p = new Path2D.Double();
        p.moveTo(0, Field.HEIGHT);
        double y = base;
        double[] ys = new double[(int) (Field.WIDTH / 8) + 1];
        for (int i = 0; i < ys.length; i++) {
            y += (r.nextDouble() - 0.5) * height * 0.6;
            y = Math.max(base - height, Math.min(base + height, y));
            ys[i] = y;
            p.lineTo(i * 8, y);
        }
        p.lineTo(Field.WIDTH, Field.HEIGHT);
        p.closePath();
        g.setColor(new Color(rgb));
        g.fill(p);
        if (snow != 0) {
            g.setClip(new Rectangle2D.Double(0, 0, Field.WIDTH, base - height / 2));
            g.setColor(new Color((snow >> 16) & 255, (snow >> 8) & 255, snow & 255, 130));
            g.fill(p);
            g.setClip(null);
        }
    }

    private static void mesas(Graphics2D g, SeededRandom r) {
        int[][] layers = {{0xD8B488, 220}, {0xC49A6C, 250}};
        for (int[] layer : layers) {
            g.setColor(new Color(layer[0]));
            double x = -20;
            while (x < Field.WIDTH) {
                double w = 40 + r.nextDouble() * 90, h = 15 + r.nextDouble() * 45;
                Path2D p = new Path2D.Double();
                p.moveTo(x, Field.HEIGHT);
                p.lineTo(x + 8, layer[1] - h);
                p.lineTo(x + w - 8, layer[1] - h);
                p.lineTo(x + w, Field.HEIGHT);
                p.closePath();
                g.fill(p);
                x += w + 20 + r.nextDouble() * 60;
            }
            g.fill(new Rectangle2D.Double(0, layer[1], Field.WIDTH, Field.HEIGHT - layer[1]));
        }
    }

    private static void stars(Graphics2D g, SeededRandom r, int count, double bottom) {
        for (int i = 0; i < count; i++) {
            double x = r.nextDouble() * Field.WIDTH, y = r.nextDouble() * bottom;
            int a = 80 + r.nextInt(170);
            g.setColor(new Color(255, 255, 255, a));
            double s = r.nextInt(6) == 0 ? 1.5 : 1;
            g.fill(new Rectangle2D.Double(x, y, s, s));
        }
    }

    /** Northern lights: three waving curtains, fading in and out along the sky. */
    private static void aurora(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (int band = 0; band < 3; band++) {
            double y0 = 70 + band * 14;
            for (double x = 160; x < 640; x += 2) {
                double fade = Math.sin((x - 160) / 480 * Math.PI);
                int alpha = (int) (44 * fade * fade);
                double top = y0 + Math.sin(x / 45 + band) * 12;
                g.setPaint(new LinearGradientPaint(0, (float) top, 0, (float) top + 34, new float[]{0f, 1f},
                        new Color[]{new Color(120, 255, 190, alpha), new Color(120, 255, 190, 0)}));
                g.fill(new Rectangle2D.Double(x, top, 2, 34));
            }
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** The green world over the moon: oceans, land and clouds, lit from the left. */
    private static void earth(Graphics2D g, double x, double y, double r) {
        glow(g, x, y, r * 1.6, 0x88BBFF, 60);
        g.setColor(new Color(0x2E5FA8));
        g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g.setClip(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g.setColor(new Color(0x4C8A44));
        g.fill(new Ellipse2D.Double(x - r * 0.6, y - r * 0.5, r * 0.8, r * 0.6));
        g.fill(new Ellipse2D.Double(x + r * 0.1, y + r * 0.1, r * 0.6, r * 0.7));
        g.setColor(new Color(255, 255, 255, 170));
        g.fill(new Ellipse2D.Double(x - r * 0.9, y - r * 0.2, r * 0.9, r * 0.18));
        g.fill(new Ellipse2D.Double(x - r * 0.2, y + r * 0.45, r * 1.0, r * 0.15));
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Ellipse2D.Double(x - r * 0.4, y - r, r * 2.2, r * 2));
        g.setClip(null);
    }
}
