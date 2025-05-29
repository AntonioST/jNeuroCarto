package io.ast.jneurocarto.core;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class RequestChannelmapException extends RuntimeException {
    public final RequestChannelmapInfo request;

    public RequestChannelmapException(RequestChannelmapInfo request) {
        super(buildMessage(request));
        this.request = request.alwaysCreate();
    }

    private static String buildMessage(RequestChannelmapInfo request) {
        String message;
        if (request.code() == null) {
            message = "request " + request.probe().getSimpleName();
        } else {
            message = "request probe " + request.code() + " from " + request.probe().getSimpleName();
        }
        return message;
    }
}
