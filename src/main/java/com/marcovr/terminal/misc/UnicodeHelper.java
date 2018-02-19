package com.marcovr.terminal.misc;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class UnicodeHelper {

    private static final String REPLACEMENT_CHAR_STR = "\ufffd";
    private static final int REPLACEMENT_CHAR_CPT = 0xfffd;

    private static ArrayList<Font> fonts;

    private enum CodePointStatus {
        SINGLE, INVALID, PAIR
    }

    public static ArrayList<Integer> stringToCodePoints(String s) {
        ArrayList<Integer> codePoints = new ArrayList<>();

        CodePointStatus status = CodePointStatus.SINGLE;
        for (int i = 0; i < s.length() - 1; i++) {
            char c1 = s.charAt(i);
            char c2 = s.charAt(i + 1);

            switch (status = checkCodePoint(c1, c2)) {
                case SINGLE:
                    codePoints.add((int) c1);
                    break;
                case PAIR:
                    codePoints.add(Character.toCodePoint(c1, c2));
                    i++;
                    break;
                case INVALID:
                    codePoints.add(REPLACEMENT_CHAR_CPT);
            }
        }

        // handle last char if not handled yet
        if (status != CodePointStatus.PAIR) {
            char c = s.charAt(s.length() - 1);
            codePoints.add(Character.isSurrogate(c) ? REPLACEMENT_CHAR_CPT : (int) c);
        }

        return codePoints;
    }

    public static ArrayList<String> splitString(String s) {
        ArrayList<String> codePoints = new ArrayList<>();

        CodePointStatus status = CodePointStatus.SINGLE;
        for (int i = 0; i < s.length() - 1; i++) {
            char c1 = s.charAt(i);
            char c2 = s.charAt(i + 1);

            switch (status = checkCodePoint(c1, c2)) {
                case SINGLE:
                    codePoints.add(String.valueOf(c1));
                    break;
                case PAIR:
                    codePoints.add(new String(new char[]{c1, c2}));
                    i++;
                    break;
                case INVALID:
                    codePoints.add(REPLACEMENT_CHAR_STR);
            }
        }

        // handle last char if not handled yet
        if (status != CodePointStatus.PAIR) {
            char c = s.charAt(s.length() - 1);
            codePoints.add(Character.isSurrogate(c) ? REPLACEMENT_CHAR_STR : String.valueOf(c));
        }

        return codePoints;
    }

    private static CodePointStatus checkCodePoint(char c1, char c2) {
        if (!Character.isSurrogate(c1)) {
            return CodePointStatus.SINGLE;
        }

        if (Character.isLowSurrogate(c1) || !Character.isLowSurrogate(c2)) {
            return CodePointStatus.INVALID;
        }

        return CodePointStatus.PAIR;
    }

    public static String codePointToReadable(int b) {
        if (Character.isBmpCodePoint(b)) {
            char c = (char) b;
            if (c == '\033') {
                return "\\e";
            }
            else if (c == 7) {
                return "BEL";
            }
            else if (c < ' ') {
                return "[" + b + "]";
            }
            else {
                return String.valueOf(c);
            }
        }
        else if (Character.isValidCodePoint(b)) {
            return new String(Character.toChars(b));
        }

        return REPLACEMENT_CHAR_STR;
    }
    
    public static String codePointToString(int b) {
        if (Character.isBmpCodePoint(b)) {
            return String.valueOf((char) b);
        }
        else if (Character.isValidCodePoint(b)) {
            return new String(Character.toChars(b));
        }

        return REPLACEMENT_CHAR_STR;
    }

    public static Font getAppropriateFont(int b, Font font) {
        if (font.canDisplay(b)) {
            return font;
        }
        else {
            for (Font f : fonts) {
                if (f.canDisplay(b)) {
                    return f.deriveFont(font.getStyle());
                }
            }
        }

        return tryAllFonts(b, font);
    }

    private static Font tryAllFonts(int b, Font font) {
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String fName : fonts) {
            Font f = new Font(fName, Font.PLAIN, 12);
            if (f.canDisplay(b)) {
                return f.deriveFont(font.getStyle());
            }
        }

        return font;
    }

    static {
        fonts = new ArrayList<>();
        int size = 12;
        fonts.add(new Font("Segoe UI Emoji", Font.PLAIN, size));
        fonts.add(new Font("MS Gothic", Font.PLAIN, size));
        fonts.add(new Font("SimSun", Font.PLAIN, size));
        fonts.add(new Font("Malgun Gothic", Font.PLAIN, size));
    }


    public static class UnicodeStreamReader extends InputStreamReader {

        private int pushBackChar = -2;

        public UnicodeStreamReader(InputStream in) {
            super(in, StandardCharsets.UTF_8);
        }

        @Override
        public int read() throws IOException {
            int c1;

            if (pushBackChar == -2) {
                c1 = super.read();
            }
            else {
                c1 = pushBackChar;
                pushBackChar = -2;
            }

            if (c1 == -1 || !Character.isSurrogate((char) c1)) {
                return c1; // c1 is an easy non-surrogate character. We're done.
            }

            if (Character.isLowSurrogate((char) c1)) {
                // c1 is a low surrogate but we need a high one.
                return REPLACEMENT_CHAR_CPT;
            }

            // We have a high-surrogate. Hopefully next is a low surrogate.
            int c2 = super.read();
            if (c2 == -1 || !Character.isLowSurrogate((char) c2)) {
                // Didn't get what we want. Push it back.
                pushBackChar = c2;
                return REPLACEMENT_CHAR_CPT;
            }

            // c1 and c2 form a surrogate pair. Join 'em.
            return Character.toCodePoint((char) c1, (char) c2);
        }

    }

}
