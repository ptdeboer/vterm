package nl.piter.vterm.channels.impl;

import java.io.IOException;
import java.io.InputStream;

public class ByteFixingInputStream extends InputStream {

    private final InputStream source;
    private final byte[] one = new byte[1];

    public ByteFixingInputStream(InputStream source) {
        this.source = source;
    }

    /**
     * Asserts that a positive byte value is returned from under-laying InputStream.
     */
    @Override
    public int read() throws IOException {
        int numRead = source.read(one);
        if (numRead < 0) {
            return numRead;
        }

        return (one[0] & 0x00ff); // mask to positive value.
    }

    public int read(byte[] buffer) throws IOException {
        return source.read(buffer);
    }

    public int read(byte[] buffer, int offset, int len) throws IOException {
        return source.read(buffer, offset, len);
    }

    // Need to check which are actually implemented:
    @Override
    public void close() throws IOException {
        source.close();
    }

    @Override
    public int available() throws IOException {
        return source.available();
    }

    @Override
    public void mark(int readLimit) {
        source.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return source.markSupported();
    }

    @Override
    public long skip(long num) throws IOException {
        return source.skip(num);
    }

}
