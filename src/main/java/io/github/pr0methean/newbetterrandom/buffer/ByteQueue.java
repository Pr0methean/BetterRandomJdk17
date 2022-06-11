package io.github.pr0methean.newbetterrandom.buffer;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public interface ByteQueue extends Closeable {

  /**
   * Blocking write of exactly {@code length} bytes; returns early if the buffer is closed or garbage-collected before
   * all bytes have been written or the current thread is interrupted. Deadlock-free provided that enough bytes will
   * eventually be polled for or the buffer will eventually die.
   *
   * An example use case is to make {@code bufferSupplier} the get method of a {@link java.lang.ref.WeakReference} or
   * {@link java.lang.ref.SoftReference}.
   *
   * @param bufferSupplier supplier that returns either the buffer to write to, or null to stop writing
   * @param source    the array to copy from
   * @param start     the first index to copy from
   * @param length    the number of bytes to write; may be more than this buffer's capacity
   */
  static int writeWhileNonNull(Supplier</* @Nullable */ ? extends ByteQueue> bufferSupplier, byte[] source,
                               int start, int length) {
    int written = 0;
    while (written < length) {
      if (Thread.currentThread().isInterrupted()) {
        return written;
      }
      final ByteQueue buffer = bufferSupplier.get();
      if (buffer == null || buffer.isClosed()) {
        break;
      }
      final int writtenThisIteration = buffer.offer(source, start + written, length - written);
      if (writtenThisIteration == 0) {
        Thread.onSpinWait();
      }
      written += writtenThisIteration;
    }
    return written;
  }

  long getCapacity();

  /**
   * Nonblocking write of up to {@code desiredLength} bytes.
   *
   * @param source        the array to copy from
   * @param start         the first index to copy from
   * @param desiredLength the maximum number of bytes to write; can be more than this buffer's
   *                      capacity, but no more than the capacity will actually be written
   * @return the number of bytes actually written
   */
  int offer(byte[] source, int start, int desiredLength);

  /**
   * Nonblocking read of up to {@code desiredLength} bytes.
   *
   * @param dest          the array to copy into
   * @param start         the first index to copy to
   * @param desiredLength the maximum number of bytes to read; can be more than this buffer's
   *                      capacity, but no more than the capacity will actually be written
   * @return the number of bytes actually read
   */
  int poll(byte[] dest, int start, int desiredLength);

  /**
   * Blocking write of exactly {@code length} bytes.
   * Deadlock-free provided that enough bytes will eventually be polled for.
   *
   * @param source the array to copy from
   * @param start the first index to copy from
   * @param length the number of bytes to write; may be more than this buffer's capacity
   * @throws InterruptedException if interrupted white waiting to write
   */
  void write(byte[] source, int start, int length) throws InterruptedException;

  /**
   * Blocking read of exactly {@code length} bytes; returns early if the buffer is garbage-collected before all
   * bytes have been written. Deadlock-free provided that enough bytes will eventually be polled for or the buffer will
   * eventually die.
   *
   * @param dest the array to copy into
   * @param start the first index to copy into
   * @param length the number of bytes to write; may be more than this buffer's capacity
   * @throws InterruptedException if interrupted white waiting to write
   */
  void read(byte[] dest, int start, int length) throws InterruptedException;

  /**
   * Indicates whether this byte queue has been closed, meaning no more can be written to it.
   * @return true if this byte queue has been closed; false otherwise
   */
  boolean isClosed();
}
