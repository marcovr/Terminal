package screen;

import java.awt.*;

public class BufferPainter {

    public static void paint(Graphics2D g, Buffer b, boolean hasFocus) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        paintBackground(g, b);
        paintForeground(g, b);
        paintCursor(g, b, hasFocus);
    }

    private static void paintBackground(Graphics2D g, Buffer b) {
        Cell c;
        for (int y = 0; y < b.getHeight(); y++) {
            for (int x = 0; x < b.getWidth(); x++) {
                c = b.getCell(x, y);
                if (c == null) {
                    continue;
                }

                g.setColor(c.background);
                fillCell(g, x, y);
            }
        }
    }

    private static void paintForeground(Graphics2D g, Buffer b) {
        Cell c;
        for (int y = 0; y < b.getHeight(); y++) {
            for (int x = 0; x < b.getWidth(); x++) {
                c = b.getCell(x, y);
                if (c == null || c.text == null) {
                    continue;
                }

                g.setColor(c.foreground);
                g.setFont(c.font);
                writeCell(g, c.text, x, y);
            }
        }
    }

    private static void paintCursor(Graphics2D g, Buffer b, boolean hasFocus) {
        Cursor cursor = b.getCursor();
        if (cursor.visible) {
            int x = cursor.x;
            int y = cursor.y;

            g.setColor(Color.GREEN);
            if (hasFocus) {
                fillCell(g, x, y);

                Cell c = b.getCell(x, y);
                if (c != null && c.text != null) {
                    g.setColor(Color.BLACK);
                    writeCell(g, c.text, x, y);
                }
            }
            else {
                drawCell(g, x, y);
            }
        }
    }

    private static void fillCell(Graphics2D g, int x, int y) {
        x = x * CellStyle.WIDTH + 1;
        y = y * CellStyle.HEIGHT + 1;
        g.fillRect(x, y, CellStyle.WIDTH, CellStyle.HEIGHT);
    }

    private static void drawCell(Graphics2D g, int x, int y) {
        x = x * CellStyle.WIDTH + 1;
        y = y * CellStyle.HEIGHT + 1;
        g.drawRect(x, y, CellStyle.WIDTH - 1, CellStyle.HEIGHT - 1);
    }

    private static void writeCell(Graphics2D g, String s, int x, int y) {
        x = x * CellStyle.WIDTH + 1;
        y = (y + 1) * CellStyle.HEIGHT - 3;
        g.drawString(s, x, y);

        if (g.getFont().isBold()) {
            g.drawString(s, x - 1, y);
        }

        /*Character.toCodePoint();
        Character.isBmpCodePoint();
        Character.isValidCodePoint();*/
    }
    
}
