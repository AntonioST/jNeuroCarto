package io.ast.jneurocarto.app.index;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

@Component
@NullMarked
public class LogMessageService {

    public interface LogMessageProvider {
        void clearMessages();

        void printMessage(String message);

        default void printMessage(List<String> message) {
            message.reversed().forEach(this::printMessage);
        }
    }

    private final List<WeakReference<LogMessageProvider>> providers = new ArrayList<>();

    public void register(LogMessageProvider provider) {
        providers.add(new WeakReference<>(provider));
    }

    public void unregister(LogMessageProvider provider) {
        var iter = providers.iterator();
        while (iter.hasNext()) {
            if (iter.next().get() == provider) {
                iter.remove();
                return;
            }
        }
    }

    public void clear() {
        LogMessageProvider provider;
        var iter = providers.iterator();
        while (iter.hasNext()) {
            if ((provider = iter.next().get()) == null) {
                iter.remove();
            } else {
                provider.clearMessages();
            }
        }
    }

    public void print(String message) {
        LogMessageProvider provider;
        var iter = providers.iterator();
        while (iter.hasNext()) {
            if ((provider = iter.next().get()) == null) {
                iter.remove();
            } else {
                provider.printMessage(message);
            }
        }
    }

    public void print(List<String> message) {
        LogMessageProvider provider;
        var iter = providers.iterator();
        while (iter.hasNext()) {
            if ((provider = iter.next().get()) == null) {
                iter.remove();
            } else {
                provider.printMessage(message);
            }
        }
    }
}
