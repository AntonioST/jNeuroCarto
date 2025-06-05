package io.ast.jneurocarto.javafx.utils;

import java.lang.ref.Cleaner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OnceForget {
    private static final Logger log = LoggerFactory.getLogger(OnceForget.class);
    private final Cleaner.Cleanable handle;

    public OnceForget(Runnable action) {
        var message = this.toString();
        log.debug("create {}", message);
        handle = Cleaner.create().register(this, () -> {
            log.debug("clean {}", message);
            action.run();
        });
    }

    public Cleaner.Cleanable getCleanHandle() {
        return handle;
    }

    public void clean() {
        handle.clean();
    }
}
