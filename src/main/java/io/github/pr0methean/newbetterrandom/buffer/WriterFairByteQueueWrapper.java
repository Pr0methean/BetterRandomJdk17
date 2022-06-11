package io.github.pr0methean.newbetterrandom.buffer;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WriterFairByteQueueWrapper extends AbstractByteQueue implements WriterFairByteQueue {
  private final ByteQueue delegate;
  private final int maxWritePerCall;
  private final long maxOfferCallsPerTurn;
  private final Lock lock;

  @Override
  public void close() throws IOException {
    super.close();
    delegate.close();
  }

  public WriterFairByteQueueWrapper(ByteQueue delegate, int maxWritePerCall, long maxAttempts) {
    this(delegate, maxWritePerCall, maxAttempts, new ReentrantLock(true));
  }

  /**
   * @param delegate a {@link ByteQueue}
   * @param maxWritePerCall the number of bytes to write per call to delegate's {@link super#offer(byte[], int, int)}
   *                        method
   * @param maxAttempts the number of offer calls to make before relinquishing the fair lock
   * @param lock a fair lock
   */
  public WriterFairByteQueueWrapper(ByteQueue delegate, int maxWritePerCall, long maxAttempts, Lock lock) {
    this.delegate = delegate;
    this.maxWritePerCall = maxWritePerCall;
    this.maxOfferCallsPerTurn = maxAttempts;
    this.lock = lock;
  }

  @Override
  public long getCapacity() {
    return delegate.getCapacity();
  }

  @Override
  public int offer(byte[] source, int start, int desiredLength) {
    try {
      lock.lockInterruptibly();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    try {
      int written = 0;
      for (long offerCalls = 0; written < desiredLength && offerCalls != maxOfferCallsPerTurn; offerCalls++) {
        if (Thread.currentThread().isInterrupted()) {
          return written;
        }
        int desiredBytes = Math.min(desiredLength - written, maxWritePerCall);
        int writtenThisTry = delegate.offer(source, start + written, desiredBytes);
        if (writtenThisTry == 0) {
          Thread.onSpinWait();
        }
        written += writtenThisTry;
      }
      return written;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public int poll(byte[] dest, int start, int desiredLength) {
    return delegate.poll(dest, start, desiredLength);
  }
}
