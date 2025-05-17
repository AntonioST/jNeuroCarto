package io.ast.jneurocarto.javafx.utils;

import java.io.IOException;

import org.slf4j.Logger;

public interface IOAction {
    void doit() throws IOException;

    static void measure(Logger log, String message, IOAction action) {
        Thread.ofVirtual().name(message).start(() -> {
            var start = System.currentTimeMillis();
            log.debug("start {}", message);
            try {
                action.doit();
            } catch (IOException e) {
                log.warn(message, e);
            }
            var pass = System.currentTimeMillis() - start;
            log.debug("stop {}. use {} sec", message, String.format("%.4f", (double) pass / 1000));
        });
    }
}
