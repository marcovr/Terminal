package com.marcovr.terminal.screen;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

public class CellStyle {

    public static final int REGULAR     = 0;
    public static final int BOLD        = 1;
    public static final int ITALIC      = 2;
    public static final int UNDERLINE   = 4;

    public static final int WIDTH = 8;
    public static final int HEIGHT = 16;

    private static Font baseFont = new Font("Courier New", Font.PLAIN,12);
    private static Font[] fonts;

    private static Color[] colors;
    private static Color foreground, background;

    static {

        int n = 8;
        fonts = new Font[n];

        Map<TextAttribute, Object> attributes;
        for (int i = 0; i < n; i++) {
            attributes = new HashMap<>();
            attributes.put(TextAttribute.WEIGHT,
                    (i & BOLD) == 0 ? TextAttribute.WEIGHT_REGULAR : TextAttribute.WEIGHT_EXTRABOLD);
            attributes.put(TextAttribute.POSTURE,
                    (i & ITALIC) == 0 ? TextAttribute.POSTURE_REGULAR : TextAttribute.POSTURE_OBLIQUE);
            attributes.put(TextAttribute.UNDERLINE,
                    (i & UNDERLINE) == 0 ? -1 : TextAttribute.UNDERLINE_ON);

            fonts[i] = baseFont.deriveFont(attributes);
        }

        colors = new Color[256];

        // 8 base colors & 8 bright colors
        int c = 0;
        int shade = 187;
        for (int i = 0; i < 8; i++) {
            int r = (i & 1) * shade;
            int g = ((i & 2) >> 1) * shade;
            int b = ((i & 4) >> 2) * shade;
            colors[c++] = new Color(r , g, b);
        }
        shade = 85;
        for (int i = 0; i < 8; i++) {
            int r = (((i & 1) << 1) + 1) * shade;
            int g = ((i & 2) + 1) * shade;
            int b = (((i & 4) >> 1) + 1) * shade;
            colors[c++] = new Color(r , g, b);
        }

        // 216 additional colors
        for (int i = 0; i < 6; i++) {
            int r = i == 0 ? 0 : i * 40 + 55;
            for (int j = 0; j < 6; j++) {
                int g = j == 0 ? 0 : j * 40 + 55;
                for (int k = 0; k < 6; k++) {
                    int b = k == 0 ? 0 : k * 40 + 55;
                    colors[c++] = new Color(r, g, b);
                }
            }
        }

        // 24 gray-scales
        for (int i = 0; i < 24; i++) {
            int g = 10 * i + 8;
            colors[c++] = new Color(g, g, g);
        }

        // default colors
        foreground = colors[7];
        background = colors[0];
    }

    public static Font getFont(int style) {
        return fonts[style];
    }

    public static Color getColor(int color) {
        return colors[color];
    }

    public static Color getForeground() {
        return foreground;
    }

    public static Color getBackground() {
        return background;
    }

}
