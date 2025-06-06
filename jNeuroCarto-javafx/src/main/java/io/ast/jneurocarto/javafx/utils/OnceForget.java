package io.ast.jneurocarto.javafx.utils;

import java.lang.ref.Cleaner;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OnceForget {
    private static final List<OnceForget> KEEP = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(OnceForget.class);

    private final String message;
    private final Cleaner.Cleanable handle;

    public OnceForget(Runnable action) {
        var message = this.toString();
        this.message = message;

        log.debug("create {}", message);
        handle = Cleaner.create().register(this, () -> {
            log.debug("clean {}", message);
            action.run();
        });
    }

    public Cleaner.Cleanable getCleanHandle() {
        return handle;
    }

    public void keepAtLeast(Duration duration) {
        keepAtLeast(duration, System::gc);
    }

    public void keepAtLeast(Duration duration, Runnable timeout) {
        if (!KEEP.contains(this)) {
            KEEP.add(this);
            Thread.ofVirtual().name("keep " + message).start(() -> {
                try {
                    Thread.sleep(duration);
                } catch (InterruptedException e) {
                } finally {
                    KEEP.remove(this);
                }
                timeout.run();
            });
        }
    }

    public void clean() {
        handle.clean();
    }
}
