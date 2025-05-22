package io.ast.jneurocarto.core.numpy;

public class UnsupportedNumpyDataFormatException extends RuntimeException {
    final NumpyHeader header;

    public UnsupportedNumpyDataFormatException(NumpyHeader header) {
        this.header = header;
    }

    public UnsupportedNumpyDataFormatException(NumpyHeader header, String message) {
        super(message);
        this.header = header;
    }

    public UnsupportedNumpyDataFormatException(NumpyHeader header, Throwable cause) {
        super(cause);
        this.header = header;
    }

    public UnsupportedNumpyDataFormatException(NumpyHeader header, String message, Throwable cause) {
        super(message, cause);
        this.header = header;
    }
}
