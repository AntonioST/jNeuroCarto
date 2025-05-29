package io.ast.jneurocarto.javafx.utils;

import java.io.IOException;

import org.slf4j.Logger;

public interface IOAction {
    void doit() throws IOException;

    static void measure(Logger log, String message, IOAction action) {
        Thread.ofVirtual().name(message).start(() -> {
            log.debug("start {}", message);
            try {
                var start = System.currentTimeMillis();
                try {
                    action.doit();
                } finally {
                    var pass = System.currentTimeMillis() - start;
                    if (pass > 10_000) {
                        log.debug("stop {}. use {} sec", message, String.format("%.4f", (double) pass / 1000));
                    } else {
                        log.debug("stop {}, use {} ms", message, pass);
                    }
                }
            } catch (IOException e) {
                log.warn(message, e);
            }
        });
    }
}
