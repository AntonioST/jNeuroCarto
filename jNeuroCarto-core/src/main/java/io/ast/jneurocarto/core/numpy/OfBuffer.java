package io.ast.jneurocarto.core.numpy;

import java.nio.ByteBuffer;

import org.jspecify.annotations.Nullable;

public class OfBuffer extends ValueArray<ByteBuffer> {
    final ByteBuffer buffer;
    int[] shape;

    public OfBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public int[] shape() {
        return shape;
    }

    @Override
    protected ByteBuffer create(NumpyHeader header) {
        this.shape = header.shape();
        return buffer;
    }

    @Override
    protected final void checkFor(ByteBuffer data) {
        if (buffer != data) throw new RuntimeException();
    }

    /**
     * this method has changed its purpose from reading value into
     * a callback of read value.
     * <br/>
     * If you have limited size of {@link #buffer}, make sure transfer the data from it
     * for next coming data.
     * <p>
     * {@snippet :
     * boolean read(ByteBuffer ret, long pos, ByteBuffer buffer) {
     *     ret.flip();
     *     // do something read from ret.
     *     ret.reset();
     *     return true;
     * }
     *}
     *
     * @param ret    {@link #buffer}
     * @param pos    the position of read data.
     * @param buffer always {@code null}.
     * @return successful
     */
    @Override
    protected boolean read(ByteBuffer ret, long pos, @Nullable ByteBuffer buffer) {
        return ret.remaining() > 0;
    }

    /**
     * this method has changed its purpose from writing value into
     * a pre-callback of written value.
     * <br/>
     * If you have limited size of {@link #buffer}, make sure transfer the data into it
     * for next writing action.
     * <p>
     * {@snippet :
     * boolean write(ByteBuffer ret, long pos, ByteBuffer buffer) {
     *     ret.reset();
     *     // do something writing into ret.
     *     ret.flip();
     *     return true;
     * }
     *}
     *
     * @param ret    {@link #buffer}
     * @param pos    the position of written data.
     * @param buffer always {@code null}.
     * @return successful
     */
    @Override
    protected boolean write(ByteBuffer ret, long pos, @Nullable ByteBuffer buffer) {
        return ret.remaining() > 0;
    }
}
