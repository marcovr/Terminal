package commands;

import java.util.Iterator;
import java.util.LinkedList;

public class NumArgAccumulator implements Iterable<Integer> {

    private LinkedList<Integer> args = new LinkedList<>();
    private boolean startnew;
    private int current;

    private static final int EMPTY = -1;

    public void start() {
        args.clear();
        startnew = true;
        current = 0;
    }

    private void next() {
        args.add(startnew ? EMPTY : current);
        current = 0;
        startnew = true;
    }

    public boolean accumulate(int b) {
        if (b >= '0' && b <= '9') {
            current *= 10;
            current += b - '0';
            startnew = false;
        }
        else if (b == ';') {
            next();
        }
        else {
            next();
            return false;
        }

        return true;
    }

    public int getArgOrDef(int i, int def) {
        if (i >= args.size()) {
            return def;
        }
        int n = args.get(i);
        return n == EMPTY ? def : n;
    }

    public int consumeArgOrDef(int def) {
        if (args.isEmpty()) {
            return def;
        }
        int n = args.removeFirst();
        return n == EMPTY ? def : n;
    }

    public boolean isEmpty() {
        return args.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            int arg = args.get(i);
            if (arg != EMPTY) {
                sb.append(arg);
            }
            if (i < args.size() - 1) {
                sb.append(';');
            }
        }
        return sb.toString();
    }

    @Override
    public Iterator<Integer> iterator() {
        return args.iterator();
    }
}
