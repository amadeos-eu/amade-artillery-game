package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.games.artillery.domain.Field;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** A tank drawn with shapes: tracks with wheels, the hull in the seat's colour, the turret and the barrel. */
final class TankArt {
    private static final Color TRACKS = new Color(0x2B2B2B);
    private static final Color WHEELS = new Color(0x5A5A5A);
    private static final Color BARREL = new Color(0x2A2A2A);
    private static final Color WRECK = new Color(0x3A3430);

    private TankArt() {
    }

    // --8<-- [start:tank]
    /** A tank whose tracks stand at {@code x}, {@code y}, its barrel at {@code angle} degrees. */
    static void paint(Graphics2D g, double x, double y, double angle, Color body, boolean alive) {
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        double rad = Math.toRadians(angle);
        double half = Field.TANK_WIDTH / 2;
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(alive ? BARREL : WRECK);
        double reach = alive ? Field.BARREL : Field.BARREL * 0.6;
        double droop = alive ? rad : Math.toRadians(angle > 90 ? 160 : 20);
        g.draw(new Line2D.Double(0, -Field.PIVOT, Math.cos(droop) * reach, -Field.PIVOT - Math.sin(droop) * reach));
        g.setStroke(new BasicStroke(1));
        g.setColor(TRACKS);
        g.fill(new RoundRectangle2D.Double(-half, -5, Field.TANK_WIDTH, 6, 6, 6));
        g.setColor(WHEELS);
        for (int i = -8; i <= 8; i += 4) g.fill(new Ellipse2D.Double(i - 1.5, -3.5, 3, 3));
        g.setColor(alive ? body : WRECK);
        g.fill(new RoundRectangle2D.Double(-half + 1, -9, Field.TANK_WIDTH - 2, 5, 3, 3));
        g.fill(new Arc2D.Double(-6, -14, 12, 10, 0, 180, Arc2D.CHORD));
        if (alive) {
            g.setColor(new Color(255, 255, 255, 70));
            g.fill(new Rectangle2D.Double(-half + 2, -9, Field.TANK_WIDTH - 4, 1.4));
        }
        g.setTransform(saved);
    }
    // --8<-- [end:tank]
}
