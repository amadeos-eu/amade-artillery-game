package eu.amadeos.games.artillery.ui.game;

import eu.amadeos.games.artillery.domain.Earth;
import eu.amadeos.games.artillery.domain.Theme;

import java.awt.Color;

/** The colours of the game: the seats, and the earth of each landscape. */
final class Look {
    /** Red, blue, green, gold. */
    static final Color[] SEATS = {new Color(0xD8473A), new Color(0x3F7FD8), new Color(0x4AA04A), new Color(0xE0B030)};

    private Look() {
    }

    static Color seat(int number) {
        return SEATS[number];
    }

    /** The colours of the earth's materials in a landscape, by material number (0 is air). */
    static int[] earth(Theme theme) {
        int[] colours = new int[8];
        switch (theme) {
            case VALLEY -> set(colours, 0x5F4A2E, 0x8FB84A, 0x7A5A36, 0x2E2620);
            case DESERT -> set(colours, 0xD4A25C, 0xF0D394, 0xB88A50, 0x4A3A2A);
            case WINTER -> set(colours, 0x646A7C, 0xF2F6FF, 0x5A5048, 0x262830);
            case MOON -> set(colours, 0x8A8A88, 0xBEBEBA, 0x76726E, 0x383838);
        }
        return colours;
    }

    private static void set(int[] colours, int soil, int top, int dirt, int scorched) {
        colours[Earth.SOIL] = soil;
        colours[Earth.TOP] = top;
        colours[Earth.DIRT] = dirt;
        colours[Earth.SCORCHED] = scorched;
    }

    /** A colour lighter (positive) or darker (negative) by a part of 1. */
    static int shade(int rgb, double amount) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        if (amount >= 0) {
            r += (int) ((255 - r) * amount);
            g += (int) ((255 - g) * amount);
            b += (int) ((255 - b) * amount);
        } else {
            r = (int) (r * (1 + amount));
            g = (int) (g * (1 + amount));
            b = (int) (b * (1 + amount));
        }
        return (r << 16) | (g << 8) | b;
    }
}
