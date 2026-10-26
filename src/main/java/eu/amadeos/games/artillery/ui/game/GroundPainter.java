package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.gameengine.terrain.Ground;
import eu.amadeos.games.artillery.domain.Earth;
import eu.amadeos.games.artillery.domain.Field;
import eu.amadeos.games.artillery.domain.Theme;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * The picture of the ground: an image with one pixel per cell of earth, painted again only in the columns
 * that changed since the last frame. Each material has its colour in the landscape, a little speckled, the
 * soil in bands; the top cell of each column is lighter, so the edge of the ground shines.
 */
final class GroundPainter {
    private Ground ground;
    private BufferedImage image;
    private int[] pixels;
    private int[] colours;

    // --8<-- [start:paint]
    /** Brings the image up to date with the ground (all of it for a new ground) and draws it over the field. */
    void paint(Graphics2D g, Ground ground, Theme theme) {
        if (ground != this.ground) {
            this.ground = ground;
            colours = Look.earth(theme);
            image = new BufferedImage(ground.columns(), ground.rows(), BufferedImage.TYPE_INT_ARGB);
            pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            ground.takeChanges();
            repaint(0, ground.columns() - 1);
        } else {
            Ground.Changes changes = ground.takeChanges();
            if (changes != null) repaint(changes.from(), changes.to());
        }
        g.drawImage(image, 0, 0, (int) Field.WIDTH, (int) Field.HEIGHT, null);
    }

    private void repaint(int from, int to) {
        int columns = ground.columns(), rows = ground.rows();
        for (int c = from; c <= to; c++) {
            boolean above = false;
            for (int r = 0; r < rows; r++) {
                byte m = ground.cell(c, r);
                int index = r * columns + c;
                if (m == Ground.AIR) {
                    pixels[index] = 0;
                    above = false;
                    continue;
                }
                int rgb = colours[m];
                double shade = speckle(c, r) * 0.07;
                if (m == Earth.SOIL && band(c, r)) shade -= 0.09;
                if (!above) shade += 0.18;                     // the edge, where light falls on it
                pixels[index] = 0xFF000000 | Look.shade(rgb, shade);
                above = true;
            }
        }
    }
    // --8<-- [end:paint]

    /** A fixed speckle per cell, -1 to 1: the same cell always looks the same. */
    private static double speckle(int c, int r) {
        int h = c * 73856093 ^ r * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return ((h & 0xFFFF) / 32767.5) - 1;
    }

    /** The bands of the soil, gently waving. */
    private boolean band(int c, int r) {
        double cell = ground.cellSize();
        double y = r * cell + Math.sin(c * cell / 37.0) * 4 + Math.sin(c * cell / 11.0);
        return ((int) Math.floor(y / 14)) % 2 == 0;
    }

    /** For tests: the colour at a cell. */
    int pixel(int column, int row) {
        return pixels[row * ground.columns() + column];
    }
}
