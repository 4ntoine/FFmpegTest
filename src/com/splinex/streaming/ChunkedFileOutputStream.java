package com.splinex.streaming;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ChunkedFileOutputStream extends OutputStream {
    private long written;
    private FileOutputStream fos;
    private String path;
    private int chunk = 0;
    private long chunkSize;

    public ChunkedFileOutputStream(String path, long chunkSize) throws FileNotFoundException {
        this.path = path;
        this.chunkSize = chunkSize;
        createNewChunk();
    }

    private void createNewChunk() throws FileNotFoundException {
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                Log.e("IOException: ", e);
            }
            chunk++;
        }
        fos = new FileOutputStream(chunk == 0 ? path : String.format("%s.%02d", path, chunk));
        written = 0;
    }

    @Override
    public void close() throws IOException {
        fos.close();
    }

    @Override
    public void flush() throws IOException {
        fos.flush();
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        if (written + buffer.length > chunkSize)
            createNewChunk();
        fos.write(buffer);
        written += buffer.length;
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        if (written + count > chunkSize)
            createNewChunk();
        fos.write(buffer, offset, count);
        written += count;
    }

    @Override
    public void write(int i) throws IOException {
        if (written >= chunkSize)
            createNewChunk();
        fos.write(i);
        written++;
    }
}
