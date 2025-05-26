package io.ast.jneurocarto.javafx.app;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class RequestChannelmapTypeException extends RuntimeException {
    public final RequestChannelmapType request;

    public RequestChannelmapTypeException(RequestChannelmapType request) {
        String message;
        if (request.code() == null) {
            message = "request " + request.probe().getSimpleName();
        } else {
            message = "request probe " + request.code() + " from " + request.probe().getSimpleName();
        }
        super(message);
        this.request = request.alwaysCreate();
    }
}
