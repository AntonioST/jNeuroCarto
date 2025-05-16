package io.ast.jneurocarto.javafx.app;

import java.util.List;

import org.jspecify.annotations.NonNull;

public final class LogMessageService {
    private LogMessageService() {
        throw new RuntimeException();
    }

    public static void printMessage(String message) {
        Application.getInstance().printMessage(message);
    }

    public static void printMessage(@NonNull List<String> message) {
        Application.getInstance().printMessage(message);
    }

    public static void clearMessages() {
        Application.getInstance().clearMessages();
    }
}
