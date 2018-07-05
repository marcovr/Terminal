package com.marcovr.terminal.screen;

import java.awt.*;

class BufferPainter {

    static void paint(Graphics2D g, Buffer b, Cursor c, boolean inverted, boolean hasFocus) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        if (inverted) {
            paintInvertedBackground(g, b);
            paintInvertedForeground(g, b);
        }
        else {
            paintBackground(g, b);
            paintForeground(g, b);
        }
        paintCursor(g, b, c, hasFocus);
    }

    private static void paintBackground(Graphics2D g, Buffer b) {
        Cell c;
        for (int y = 0; y < b.height; y++) {
            for (int x = 0; x < b.width; x++) {
                c = b.cells[y][x];
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
        for (int y = 0; y < b.height; y++) {
            for (int x = 0; x < b.width; x++) {
                c = b.cells[y][x];
                if (c == null || c.text == null) {
                    continue;
                }

                g.setColor(c.foreground);
                g.setFont(c.font);
                writeCell(g, c.text, x, y);
            }
        }
    }

    private static void paintInvertedBackground(Graphics2D g, Buffer b) {
        Cell c;
        for (int y = 0; y < b.height; y++) {
            for (int x = 0; x < b.width; x++) {
                c = b.cells[y][x];
                if (c == null) {
                    continue;
                }

                g.setColor(c.foreground);
                fillCell(g, x, y);
            }
        }
    }

    private static void paintInvertedForeground(Graphics2D g, Buffer b) {
        Cell c;
        for (int y = 0; y < b.height; y++) {
            for (int x = 0; x < b.width; x++) {
                c = b.cells[y][x];
                if (c == null || c.text == null) {
                    continue;
                }

                g.setColor(c.background);
                g.setFont(c.font);
                writeCell(g, c.text, x, y);
            }
        }
    }

    private static void paintCursor(Graphics2D g, Buffer b, Cursor cursor, boolean hasFocus) {
        if (cursor.visible) {
            int x = cursor.getX();
            int y = cursor.getY();

            g.setColor(Color.GREEN);
            if (hasFocus) {
                fillCell(g, x, y);

                Cell c = b.cells[y][x];
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
    }
    
}
