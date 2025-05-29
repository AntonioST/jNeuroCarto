package io.ast.jneurocarto.javafx.utils;

import javafx.scene.input.KeyCode;

/**
 * Provide a string buffer that collects characters in a short period of time.
 * If the interval typing over a max waiting time, then the buffer will reset
 * the content.
 */
public class FlashStringBuffer {

    private final long wait;
    private final StringBuilder buffer = new StringBuilder();
    private long last;

    public FlashStringBuffer() {
        this(1000);
    }

    public FlashStringBuffer(long wait) {
        this.wait = wait;
    }

    /**
     * Accept key code into a buffer.
     * It accepts all letter, number, space, backspace and escape.
     *
     * @param c
     * @return Did {@code c} accept?
     */
    public boolean accept(KeyCode c) {
        var current = System.currentTimeMillis();
        if (current - last > wait) {
            reset();
        }

        if (c.isLetterKey() || c.isDigitKey()) {
            buffer.append(c.getChar());
        } else if (c == KeyCode.SPACE) {
            if (!buffer.isEmpty()) {
                buffer.append(' ');
            }
        } else if (c == KeyCode.BACK_SPACE) {
            buffer.deleteCharAt(buffer.length() - 1);
        } else if (c == KeyCode.ESCAPE) {
            reset();
            return true;
        } else {
            return false;
        }

        last = current;
        return true;
    }

    public void reset() {
        buffer.delete(0, buffer.length());
        last = 0;
    }

    public String toString() {
        return buffer.toString();
    }
}
