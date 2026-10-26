package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.gameengine.physics.Projectile;
import eu.amadeos.gameengine.random.SeededRandom;
import eu.amadeos.games.artillery.domain.Battle;
import eu.amadeos.games.artillery.domain.Field;
import eu.amadeos.games.artillery.domain.Tank;
import eu.amadeos.games.artillery.domain.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Random;

/**
 * Draws a battle in logical coordinates: the backdrop of the landscape, the ground, the dotted line of the
 * last shot, the tanks, the shells, the blasts, the particles and the weather.
 */
final class Painter {
    private final Backdrop backdrop = new Backdrop();
    private final GroundPainter ground = new GroundPainter();
    private final double[] flakes = new double[300];

    Painter() {
        Random r = new Random(5);
        for (int i = 0; i < flakes.length; i++) flakes[i] = r.nextDouble();
    }

    // --8<-- [start:paint]
    /** {@code aim} is where a person drags the mouse to aim, or {@code null}. */
    void paint(Graphics2D g, Battle battle, Effects effects, double alpha, Point2D aim) {
        AffineTransform saved = g.getTransform();
        double shake = effects.shake();
        if (shake > 0) {
            SeededRandom r = new SeededRandom((long) (effects.time() * 60));
            g.translate((r.nextDouble() - 0.5) * shake, (r.nextDouble() - 0.5) * shake);
        }
        backdrop.draw(g, battle.theme());
        ground.paint(g, battle.ground(), battle.theme());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Tank current = battle.current();
        if (battle.phase() == Battle.Phase.AIM) lastShot(g, current);
        for (Tank t : battle.tanks()) {
            TankArt.paint(g, t.x(), t.y(alpha), t.angle(), Look.seat(t.seat().number()), t.alive());
        }
        if (aim != null && battle.phase() == Battle.Phase.AIM) aimGuide(g, current, alpha);
        for (Battle.Shell shell : battle.shells()) shell(g, shell.projectile(), alpha);
        for (Battle.Blast blast : battle.blasts()) blast(g, blast);
        effects.render(g, alpha);
        weather(g, battle.theme(), battle.wind(), effects.time());
        g.setTransform(saved);
    }
    // --8<-- [end:paint]

    /** The dots of the last shot of the tank whose turn it is, so its player can correct the aim. */
    private static void lastShot(Graphics2D g, Tank tank) {
        List<double[]> points = tank.lastShot();
        Color c = Look.seat(tank.seat().number());
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 170));
        for (double[] p : points) g.fill(new Ellipse2D.Double(p[0] - 0.9, p[1] - 0.9, 1.8, 1.8));
    }

    /** While a person aims with the mouse: a line from the turret in the barrel's direction, as long as the power. */
    private static void aimGuide(Graphics2D g, Tank tank, double alpha) {
        double rad = Math.toRadians(tank.angle());
        double length = 20 + tank.power() * 1.4;
        double x = tank.pivotX(), y = tank.y(alpha) - Field.PIVOT;
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{3, 3}, 0));
        g.setColor(new Color(255, 255, 255, 170));
        g.draw(new Line2D.Double(x, y, x + Math.cos(rad) * length, y - Math.sin(rad) * length));
        g.setStroke(new BasicStroke(1));
    }

    private static void shell(Graphics2D g, Projectile p, double alpha) {
        AffineTransform saved = g.getTransform();
        g.translate(p.x(alpha), p.y(alpha));
        g.rotate(p.heading());
        g.setColor(new Color(255, 210, 122, 90));
        g.fill(new Ellipse2D.Double(-7, -2.5, 10, 5));
        g.setColor(new Color(0x303030));
        g.fill(new RoundRectangle2D.Double(-3, -1.5, 6.5, 3, 3, 3));
        g.setTransform(saved);
    }

    // --8<-- [start:blast]
    /** A ball of fire that swells to the blast's size in a fifth of its time, then fades. */
    private static void blast(Graphics2D g, Battle.Blast blast) {
        double t = blast.progress();
        double grow = Math.min(1, t / 0.2);
        double r = blast.radius() * (0.4 + 0.75 * grow);
        int alpha = (int) (255 * (1 - t));
        if (r <= 0 || alpha <= 0) return;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(blast.x(), blast.y()), (float) r, new float[]{0f, 0.35f, 0.75f, 1f},
                new Color[]{new Color(255, 255, 230, alpha), new Color(255, 214, 90, alpha),
                        new Color(255, 106, 32, alpha * 3 / 4), new Color(150, 40, 20, 0)}));
        g.fill(new Ellipse2D.Double(blast.x() - r, blast.y() - r, r * 2, r * 2));
    }
    // --8<-- [end:blast]

    /** Snow over the winter peaks, sand blowing over the dunes, both with the wind. */
    private void weather(Graphics2D g, Theme theme, int wind, double time) {
        if (theme == Theme.WINTER) {
            g.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < 140; i++) {
                double x = (flakes[i] * Field.WIDTH + time * wind * 3 + Math.sin(time + i) * 6) % Field.WIDTH;
                if (x < 0) x += Field.WIDTH;
                double y = (flakes[(i * 7) % flakes.length] * Field.HEIGHT + time * (20 + (i % 5) * 4)) % Field.HEIGHT;
                double s = 1 + (i % 3) * 0.4;
                g.fill(new Rectangle2D.Double(x, y, s, s));
            }
        } else if (theme == Theme.DESERT && Math.abs(wind) > 2) {
            g.setColor(new Color(250, 230, 190, (int) Math.min(90, Math.abs(wind) * 8)));
            for (int i = 0; i < 40; i++) {
                double x = (flakes[i] * Field.WIDTH * 1.5 + time * wind * 14) % (Field.WIDTH * 1.5);
                if (x < 0) x += Field.WIDTH * 1.5;
                x -= Field.WIDTH * 0.25;
                double y = 200 + flakes[i + 60] * 190;
                g.fill(new Rectangle2D.Double(x, y, Math.abs(wind) * 2.5, 0.8));
            }
        }
    }
}
