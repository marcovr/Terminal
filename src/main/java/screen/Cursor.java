package screen;

import misc.UnicodeHelper;

import java.awt.*;

public class Cursor {

    private Buffer buffer;
    public int x = 0, y = 0;
    public boolean visible = true;
    public boolean blinking = false;

    public Color foreground = CellStyle.getForeground();
    public Color background = CellStyle.getBackground();
    public int style = CellStyle.REGULAR;

    public Cursor(Buffer buffer) {
        this.buffer = buffer;
    }

    public Cursor copy() {
        Cursor c = new Cursor(buffer);
        c.x = x;
        c.y = y;
        c.visible = visible;
        c.blinking = blinking;
        c.foreground = foreground;
        c.background = background;
        c.style = style;
        return c;
    }

    public void up(int n) {
        y = Math.max(y - n, 0);
    }

    public void down(int n) {
        y = Math.min(y + n, buffer.getHeight());
    }

    public void left(int n) {
        x = Math.max(x - n, 0);
    }

    public void right(int n) {
        x = Math.min(x + n, buffer.getWidth());
    }

    public void next() {
        if (x + 1 == buffer.getWidth()) {
            if (buffer.autoWrap) {
                x = 0;
                lineFeed();
            }
        }
        else {
            x++;
        }
    }

    public void prev() {
        if (x == 0) {
            if (buffer.autoWrap) {
                x = buffer.getWidth() - 1;
                y--;
                if (y < buffer.scrollTop) {
                    buffer.scroll(-1);
                    y++;
                }
            }
        }
        else {
            x--;
        }
    }

    public void lineFeed() {
        if (y + 1 == buffer.scrollBottom) {
            buffer.scroll(1);
        }
        else {
            y++;
        }
    }

    public void CR_LF() {
        x = 0;
        lineFeed();
    }

    public void write(int b) {
        buffer.clearSelection();
        _write(b);
    }

    public void write(String s) {
        buffer.clearSelection();
        for (int c : UnicodeHelper.stringToCodePoints(s)) {
            _write(c);
        }
    }

    public void writeBlank() {
        _write(' ');
    }

    private void _write(int c) {
        if (writeDiacritic(c)) {
            return;
        }

        Cell cell = buffer.getCell(x, y);
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
                Cell cell = buffer.getCell(x, y);
                cell.text += UnicodeHelper.codePointToString(c);
                next();
                return true;
        }
        return false;
    }

    public void delete(int n) {
        buffer.delete(n, x, y);
    }

    public void invertColors() {
        Color temp = background;
        background = foreground;
        foreground = temp;
    }
}
