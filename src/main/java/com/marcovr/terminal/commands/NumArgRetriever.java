package com.marcovr.terminal.commands;

import com.marcovr.terminal.ssh.ConnectionHandler;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Allows to create a list of integer arguments out of characters read from ConnectionHandler
 */
class NumArgRetriever implements Iterable<Integer> {

    private final LinkedList<Integer> args = new LinkedList<>();
    private final ConnectionHandler handler;

    private static final int EMPTY = -1;

    /**
     * Creates a new NumArgRetriever
     *
     * @param handler the ConnectionHandler to read from
     */
    NumArgRetriever(ConnectionHandler handler) {
        this.handler = handler;
    }

    /**
     * Retrieves arguments from connection
     *
     * @return last character read
     * @throws IOException from connection
     */
    int retrieve() throws IOException {
        args.clear();
        boolean newArg = true;
        int currentValue = 0;
        int b;

        while (true) {
            b = handler.receive();

            if (b >= '0' && b <= '9') {
                currentValue *= 10;
                currentValue += b - '0';
                newArg = false;
            }
            else if (b == ';') {
                args.add(newArg ? EMPTY : currentValue);
                currentValue = 0;
                newArg = true;
            }
            else {
                args.add(newArg ? EMPTY : currentValue);
                return b;
            }
        }
    }

    /**
     * Returns the stored argument at position i, or default value if empty
     *
     * @param i the argument's position
     * @param def the default value
     * @return the argument
     */
    int getArgOrDef(int i, int def) {
        if (i >= args.size()) {
            return def;
        }
        int n = args.get(i);
        return n == EMPTY ? def : n;
    }

    /**
     * Returns the first stored argument, or default value if empty
     *
     * @param def the default value
     * @return the argument
     */
    int consumeArgOrDef(int def) {
        if (args.isEmpty()) {
            return def;
        }
        int n = args.removeFirst();
        return n == EMPTY ? def : n;
    }

    /**
     * @return are there arguments stored
     */
    boolean hasArguments() {
        return !args.isEmpty();
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
