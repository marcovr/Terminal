package screen;

import java.awt.*;

public class Cell {

    public String text;
    public Color foreground, background;
    public Font font;

    public Cell(Color background, Color foreground) {
        this.background = background;
        this.foreground = foreground;
    }

}
