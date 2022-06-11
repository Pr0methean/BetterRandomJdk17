package io.github.pr0methean.newbetterrandom.buffer;

import java.io.IOException;
import java.lang.ref.Cleaner;

public abstract class AbstractByteQueue implements ByteQueue {

  protected volatile boolean closed;

  @Override
  public void write(byte[] source, int start, int length) throws InterruptedException {
    int written = 0;
    while (written < length) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      final int writtenThisIteration = offer(source, start + written, length - written);
      if (writtenThisIteration == 0) {
        if (isClosed()) {
          throw new IllegalStateException("Closed");
        }
        Thread.onSpinWait();
      }
      written += writtenThisIteration;
    }
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public void close() {
    closed = true;
  }

  @Override
  public void read(byte[] dest, int start, int length) throws InterruptedException {
    int read = 0;
    while (read < length) {
      final int readThisIteration = poll(dest, start + read, length - read);
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      if (readThisIteration == 0) {
        if (isClosed()) {
          throw new IllegalStateException("Closed");
        }
        Thread.onSpinWait();
      }
      read += readThisIteration;
    }
  }
}
