package com.marcovr.terminal.screen;

import java.awt.*;

class Cell {

    String text;
    Color foreground, background;
    Font font;

    Cell(Color background, Color foreground) {
        this.background = background;
        this.foreground = foreground;
    }

}
