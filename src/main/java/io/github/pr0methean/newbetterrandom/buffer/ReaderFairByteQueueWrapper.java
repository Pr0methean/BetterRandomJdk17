package io.github.pr0methean.newbetterrandom.buffer;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderFairByteQueueWrapper extends AbstractByteQueue implements ReaderFairByteQueue {
  private final ByteQueue delegate;
  private final int maxReadPerTurn;
  private final Lock lock;
  private final long maxPollCallsPerTurn;

  @Override
  public void close() {
    super.close();
    delegate.close();
  }

  public ReaderFairByteQueueWrapper(ByteQueue delegate, int maxReadPerTurn, long maxPollCallsPerTurn) {
    this(delegate, maxReadPerTurn, maxPollCallsPerTurn, new ReentrantLock(true));
  }

  public ReaderFairByteQueueWrapper(ByteQueue delegate, int maxReadPerTurn, long maxPollCallsPerTurn, final Lock lock) {
    this.delegate = delegate;
    this.maxReadPerTurn = maxReadPerTurn;
    this.maxPollCallsPerTurn = maxPollCallsPerTurn;
    this.lock = lock;
  }

  @Override
  public long getCapacity() {
    return delegate.getCapacity();
  }

  @Override
  public int offer(byte[] source, int start, int desiredLength) {
    return delegate.offer(source, start, desiredLength);
  }

  @Override
  public int poll(byte[] dest, int start, int desiredLength) {
    if (desiredLength == 0) {
      return 0;
    }
    try {
      lock.lockInterruptibly();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return 0;
    }
    try {
      int read = 0;
      for (long pollCalls = 0; read < desiredLength && pollCalls != maxPollCallsPerTurn; pollCalls++) {
        if (Thread.currentThread().isInterrupted()) {
          return read;
        }
        int desiredThisTurn = Math.min(desiredLength - read, maxReadPerTurn);
        int readThisTurn = delegate.poll(dest, start + read, desiredThisTurn);
        if (readThisTurn == 0) {
          Thread.onSpinWait();
        }
        read += readThisTurn;
      }
      return read;
    } finally {
      lock.unlock();
    }
  }
}
