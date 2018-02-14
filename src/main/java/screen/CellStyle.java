package screen;

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

    private static Color[] foregrounds, backgrounds;
    private static final int DEFAULT_COLOR = 9;

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

        foregrounds = new Color[18];
        backgrounds = new Color[18];

        int shade = 187;
        for (int i = 0; i < 8; i++) {
            int r = (i & 1) * shade;
            int g = ((i & 2) >> 1) * shade;
            int b = ((i & 4) >> 2) * shade;
            foregrounds[i] = new Color(r , g, b);
        }
        foregrounds[8] = Color.WHITE;
        shade = 85;
        for (int i = 0; i < 8; i++) {
            int r = (((i & 1) << 1) + 1) * shade;
            int g = ((i & 2) + 1) * shade;
            int b = (((i & 4) >> 1) + 1) * shade;
            foregrounds[i + 10] = new Color(r , g, b);
        }
        foregrounds[DEFAULT_COLOR] = foregrounds[7];
        System.arraycopy(foregrounds, 0, backgrounds, 0, 18);
        backgrounds[DEFAULT_COLOR] = backgrounds[0];
    }

    public static Font getFont(int style) {
        return fonts[style];
    }

    public static Color getForeground(int color) {
        return foregrounds[color];
    }

    public static Color getBackground(int color) {
        return backgrounds[color];
    }

    public static Color getForeground() {
        return foregrounds[DEFAULT_COLOR];
    }

    public static Color getBackground() {
        return backgrounds[DEFAULT_COLOR];
    }

}
