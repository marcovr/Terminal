package com.marcovr.terminal.screen;

import java.awt.*;

public class Screen {

    private final Buffer buffer;
    private Cursor cursor;
    private Cursor savedCursor;

    public int scrollTop, scrollBottom;
    public boolean autoWrap, replaceMode, inverted;

    public Screen() {
        this(80, 24);
    }

    public Screen(int width, int height) {
        buffer = new Buffer(width, height);
        cursor = savedCursor = new Cursor(this, buffer);

        scrollTop = 0;
        scrollBottom = height;

        autoWrap = replaceMode = true;
        inverted = false;
    }

    public int getWidth() {
        return buffer.width;
    }

    public int getHeight() {
        return buffer.height;
    }

    public synchronized void resize(int width, int height) {
        if (width != buffer.width || height != buffer.height) {
            buffer.clearSelection();
            if (cursor.y >= height) {
                _scroll(cursor.y - (height - 1));
            }

            scrollBottom += height - buffer.height;
            buffer.resize(width, height);
            cursor.x = Math.min(cursor.x, width - 1);
            cursor.y = Math.min(cursor.y, height - 1);
        }
    }

    public synchronized void clear() {
        buffer.clearSelection();
        cursor.clear();
    }

    public synchronized void clearLine(int y) {
        buffer.clearSelection();
        cursor.clearLine(y);
    }

    public synchronized void clearCell(int x, int y) {
        buffer.clearSelection();
        cursor.clearCell(x, y);
    }

    public synchronized void scroll(int d) {
        buffer.clearSelection();
        _scroll(d);
    }

    private void _scroll(int d) {
        if (d > 0) {
            System.arraycopy(buffer.cells, scrollTop + d, buffer.cells, scrollTop, scrollBottom - d);
            for (int i = scrollBottom - d; i < scrollBottom; i++) {
                buffer.cells[i] = new Cell[buffer.width];
                cursor.clearLine(i);
            }
        }
        else {
            d = -d;
            System.arraycopy(buffer.cells, scrollTop, buffer.cells, scrollTop + d, scrollBottom - d);
            for (int i = scrollTop; i < scrollTop + d; i++) {
                buffer.cells[i] = new Cell[buffer.width];
                cursor.clearLine(i);
            }
        }
    }

    public synchronized void useAlternateBuffer() {
        buffer.clearSelection();
        buffer.useAlternate();
    }

    public synchronized void useNormalBuffer() {
        buffer.clearSelection();
        buffer.useNormal();
    }

    public Cursor getCursor() {
        return cursor;
    }

    public synchronized void saveCursor() {
        savedCursor = cursor.copy();
    }

    public synchronized void restoreCursor() {
        cursor = savedCursor;
    }

    public synchronized void delete(int n) {
        buffer.clearSelection();
        cursor.delete(n);
    }

    public synchronized String getSelection() {
        return buffer.getSelection();
    }

    public synchronized void select(Point start, Point end) {
        buffer.clearSelection();
        buffer.select(pixelToCell(start), pixelToCell(end));
    }

    private Point pixelToCell(Point p) {
        p.x = Math.min(Math.max(p.x / CellStyle.WIDTH, 0), buffer.width - 1);
        p.y = Math.min(Math.max(p.y / CellStyle.HEIGHT, 0), buffer.height - 1);
        return p;
    }

    public synchronized void clearSelection() {
        buffer.clearSelection();
    }

    public synchronized void write(int b) {
        buffer.clearSelection();
        cursor.write(b);
    }

    public synchronized void write(String s) {
        buffer.clearSelection();
        cursor.write(s);
    }

    public synchronized void writeBlank() {
        cursor.write(' ');
    }

    public synchronized void paint(Graphics2D g, boolean hasFocus) {
        BufferPainter.paint(g, buffer, cursor, inverted, hasFocus);
    }
}
