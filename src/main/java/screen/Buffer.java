package screen;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Buffer {

    private int width, height;
    private Cell[][] cells;
    private Cell[][][] buffers;
    private Cursor cursor;
    private Cursor savedCursor;
    private boolean alternate;

    private Point selectionStart, selectionEnd;

    public int scrollTop, scrollBottom;
    public boolean autoWrap, replaceMode;

    public AtomicBoolean tainted;

    public Buffer() {
        this(80, 24);
    }

    public Buffer(int width, int height) {
        this.width = width;
        this.height = height;

        scrollTop = 0;
        scrollBottom = height;
        autoWrap = replaceMode = true;

        tainted = new AtomicBoolean();

        cursor = savedCursor = new Cursor(this);

        buffers = new Cell[2][][];
        cells = new Cell[height][width];
        clear();
        buffers[1] = cells;
        cells = new Cell[height][width];
        clear();
        buffers[0] = cells;
    }

    public Cursor getCursor() {
        return cursor;
    }

    public Cell getCell(int x, int y) {
        return cells[y][x];
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        resize(width, height);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        resize(width, height);
    }

    public void resize(int width, int height) {
        clearSelection();
        // TODO: lock buffer during resize

        buffers[0] = _resize(buffers[0], width, height);
        buffers[1] = _resize(buffers[1], width, height);

        cells = alternate ? buffers[1] : buffers[0];

        scrollBottom += height - this.height;
        this.width = width;
        this.height = height;
    }

    private Cell[][] _resize(Cell[][] buffer, int newwidth, int newheight) {
        int minw = Math.min(width, newwidth);
        int minh = Math.min(height, newheight);

        Cell[][] temp = new Cell[newheight][newwidth];
        for (int y = 0; y < minh; y++) {
            System.arraycopy(buffer[y], 0, temp[y], 0, minw);
            if (width < newwidth) {
                for (int x = width; x < newwidth; x++) {
                    // TODO: use default colors, not cursor colors
                    temp[y][x] = new Cell(cursor.background, cursor.foreground);
                }
            }
        }
        if (height < newheight) {
            for (int y = height; y < newheight; y++) {
                for (int x = 0; x < newwidth; x++) {
                    // TODO: use default colors, not cursor colors
                    temp[y][x] = new Cell(cursor.background, cursor.foreground);
                }
            }
        }

        return temp;
    }

    public void clear() {
        clearSelection();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                cells[y][x] = new Cell(cursor.background, cursor.foreground);
            }
        }
    }

    public void clearLine(int y) {
        clearSelection();
        for (int x = 0; x < width; x++) {
            cells[y][x] = new Cell(cursor.background, cursor.foreground);
        }
    }

    public void clearCell(int x, int y) {
        clearSelection();
        cells[y][x] = new Cell(cursor.background, cursor.foreground);
    }

    public void scroll(int d) {
        clearSelection();
        if (d > 0) {
            System.arraycopy(cells, scrollTop + d, cells, scrollTop, scrollBottom - d);
            for (int i = scrollBottom - d; i < scrollBottom; i++) {
                cells[i] = new Cell[width];
                clearLine(i);
            }
        }
        else {
            d = -d;
            System.arraycopy(cells, scrollTop, cells, scrollTop + d, scrollBottom - d);
            for (int i = scrollTop; i < scrollTop + d; i++) {
                cells[i] = new Cell[width];
                clearLine(i);
            }
        }
    }

    public void useAlternate() {
        clearSelection();
        alternate = true;
        cells = buffers[1];
    }

    public void useNormal() {
        clearSelection();
        alternate = false;
        cells = buffers[0];
    }

    public void saveCursor() {
        savedCursor = cursor.copy();
    }

    public void restoreCursor() {
        cursor = savedCursor;
    }

    public void delete(int n, int x, int y) {
        clearSelection();

        Cell[] line = cells[y];
        int w = x + n;
        System.arraycopy(line, w, line, x, width - 1 - w);
        for (int i = width - 1 - w; i < width; i++) {
            cells[y][i] = new Cell(cursor.background, cursor.foreground);
        }
    }

    public String getSelection() {
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

    public void select(Point start, Point end) {
        clearSelection();

        start = pixelToCell(start);
        end = pixelToCell(end);

        if (start.y < end.y || start.y == end.y && start.x < end.x) {
            selectionStart = start;
            selectionEnd = end;
        }
        else {
            selectionStart = end;
            selectionEnd = start;
        }

        markSelection();
        tainted.set(true);
    }

    private Point pixelToCell(Point p) {
        p.x = Math.min(Math.max(p.x / CellStyle.WIDTH, 0), width - 1);
        p.y = Math.min(Math.max(p.y / CellStyle.HEIGHT, 0), height - 1);
        return p;
    }

    public void clearSelection() {
        if (selectionStart != null) {
            markSelection();
            selectionStart = null;
            tainted.set(true);
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
