package com.marcovr.terminal.screen;

import java.awt.*;

class Buffer {

    int width, height;
    Cell[][] cells;

    private Cell[][][] buffers;
    private boolean alternate;
    private Point selectionStart, selectionEnd;

    Buffer(int width, int height) {
        this.width = width;
        this.height = height;

        buffers = new Cell[2][][];
        cells = new Cell[height][width];
        init();
        buffers[1] = cells;
        cells = new Cell[height][width];
        init();
        buffers[0] = cells;
    }

    void resize(int width, int height) {
        buffers[0] = _resize(buffers[0], width, height);
        buffers[1] = _resize(buffers[1], width, height);

        cells = alternate ? buffers[1] : buffers[0];

        this.width = width;
        this.height = height;
    }

    private Cell[][] _resize(Cell[][] buffer, int newWidth, int newHeight) {
        int minW = Math.min(width, newWidth);
        int minH = Math.min(height, newHeight);

        Color background = CellStyle.getBackground();
        Color foreground = CellStyle.getForeground();

        Cell[][] temp = new Cell[newHeight][newWidth];
        for (int y = 0; y < minH; y++) {
            System.arraycopy(buffer[y], 0, temp[y], 0, minW);
            if (width < newWidth) {
                for (int x = width; x < newWidth; x++) {
                    temp[y][x] = new Cell(background, foreground);
                }
            }
        }
        if (height < newHeight) {
            for (int y = height; y < newHeight; y++) {
                for (int x = 0; x < newWidth; x++) {
                    temp[y][x] = new Cell(background, foreground);
                }
            }
        }

        return temp;
    }

    private void init() {
        Color background = CellStyle.getBackground();
        Color foreground = CellStyle.getForeground();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = new Cell(background, foreground);
            }
        }
    }

    void useAlternate() {
        alternate = true;
        cells = buffers[1];
    }

    void useNormal() {
        alternate = false;
        cells = buffers[0];
    }

    String getSelection() {
        if (selectionStart == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        int x = selectionStart.x;
        int y = selectionStart.y;
        while (y < selectionEnd.y || y == selectionEnd.y && x <= selectionEnd.x) {
            Cell c = cells[y][x];
            if (c.text != null) {
                sb.append(c.text);
            }
            x++;
            if (x == width) {
                x = 0;
                y++;
                if (y <= selectionEnd.y) {
                    sb.append("\r\n");
                }
            }
        }

        return sb.toString();
    }

    void select(Point start, Point end) {
        if (start.y < end.y || start.y == end.y && start.x < end.x) {
            selectionStart = start;
            selectionEnd = end;
        }
        else {
            selectionStart = end;
            selectionEnd = start;
        }

        markSelection();
    }

    void clearSelection() {
        if (selectionStart != null) {
            markSelection();
            selectionStart = null;
        }
    }

    private void markSelection() {
        int x = selectionStart.x;
        int y = selectionStart.y;
        while (y < selectionEnd.y || y == selectionEnd.y && x <= selectionEnd.x) {
            Cell c = cells[y][x];
            Color temp = c.background;
            c.background = c.foreground;
            c.foreground = temp;

            x++;
            if (x == width) {
                x = 0;
                y++;
            }
        }
    }
}
