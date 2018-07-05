package com.marcovr.terminal.screen;

import com.marcovr.terminal.misc.UnicodeHelper;

import java.awt.*;

public class Cursor {

    private Screen screen;
    private Buffer buffer;
    private int x = 0, y = 0;

    private boolean wrapDue = false;

    public boolean visible = true;
    public boolean blinking = false;

    public Color foreground = CellStyle.getForeground();
    public Color background = CellStyle.getBackground();
    public int style = CellStyle.REGULAR;

    Cursor(Screen screen, Buffer buffer) {
        this.screen = screen;
        this.buffer = buffer;
    }

    Cursor copy() {
        Cursor c = new Cursor(screen, buffer);
        c.x = x;
        c.y = y;
        c.wrapDue = wrapDue;
        c.visible = visible;
        c.blinking = blinking;
        c.foreground = foreground;
        c.background = background;
        c.style = style;
        return c;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
        wrapDue = false;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void up(int n) {
        y = Math.max(y - n, 0);
    }

    public void down(int n) {
        y = Math.min(y + n, buffer.height);
    }

    public void left(int n) {
        x = Math.max(x - n, 0);
        wrapDue = false;
    }

    public void right(int n) {
        x = Math.min(x + n, buffer.width);
        wrapDue = false;
    }

    public void next() {
        if (x + 1 == buffer.width) {
            wrapDue = screen.autoWrap;
        }
        else {
            x++;
            wrapDue = false;
        }
    }

    public void prev() {
        if (x == 0) {
            wrapDue = screen.autoWrap;
            if (screen.autoWrap) {
                x = buffer.width - 1;
                y--;
                if (y < screen.scrollTop) {
                    screen.scroll(-1);
                    y++;
                }
            }
        }
        else {
            x--;
            wrapDue = false;
        }
    }

    public void carriageReturn() {
        x = 0;
        wrapDue = false;
    }

    public void lineFeed() {
        if (y + 1 == screen.scrollBottom) {
            screen.scroll(1);
        }
        else {
            y++;
        }
    }

    public void CR_LF() {
        carriageReturn();
        lineFeed();
    }

    void write(String s) {
        for (int c : UnicodeHelper.stringToCodePoints(s)) {
            write(c);
        }
    }

    void write(int c) {
        if (wrapDue && screen.autoWrap) {
            CR_LF();
        }

        if (writeDiacritic(c)) {
            return;
        }

        Cell cell = buffer.cells[y][x];
        cell.foreground = foreground;
        cell.background = background;
        cell.font = UnicodeHelper.getAppropriateFont(c, CellStyle.getFont(style));
        cell.text = UnicodeHelper.codePointToString(c);
        next();
    }

    private boolean writeDiacritic(int c) {
        switch (Character.getType(c)) {
            case Character.NON_SPACING_MARK:
            case Character.COMBINING_SPACING_MARK:
                prev();
                Cell cell = buffer.cells[y][x];
                cell.text += UnicodeHelper.codePointToString(c);
                next();
                return true;
        }
        return false;
    }

    void delete(int n) {
        wrapDue = false;
        Cell[] line = buffer.cells[y];
        int w = x + n;
        System.arraycopy(line, w, line, x, buffer.width - 1 - w);
        for (int i = buffer.width - 1 - w; i < buffer.width; i++) {
            clearCell(i, y);
        }
    }

    public void invertColors() {
        Color temp = background;
        background = foreground;
        foreground = temp;
    }

    void clear() {
        for (int y = 0; y < buffer.height; y++) {
            clearLine(y);
        }
    }

    void clearLine(int y) {
        for (int x = 0; x < buffer.width; x++) {
            clearCell(x, y);
        }
    }

    void clearCell(int x, int y) {
        buffer.cells[y][x] = new Cell(background, foreground);
    }
}
