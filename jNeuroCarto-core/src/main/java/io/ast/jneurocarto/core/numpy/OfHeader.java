package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

final class OfHeader extends ValueArray<NumpyHeader> {

    private NumpyHeader header;

    @Override
    protected NumpyHeader create(NumpyHeader header) {
        this.header = header;
        return header;
    }

    @Override
    protected void checkFor(NumpyHeader data) {
        throw new UnsupportedOperationException("do not support for writing");
    }

    @Override
    public int[] shape() {
        return header.shape();
    }

    @Override
    protected boolean read(NumpyHeader ret, long pos, ByteBuffer buffer) {
        throw new UnsupportedOperationException("special case.");
    }

    @Override
    protected boolean write(NumpyHeader ret, long pos, ByteBuffer buffer) {
        throw new UnsupportedOperationException("do not support for writing");
    }
}
