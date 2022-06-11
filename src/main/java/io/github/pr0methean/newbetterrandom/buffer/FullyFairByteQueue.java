package io.github.pr0methean.newbetterrandom.buffer;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FullyFairByteQueue extends AbstractByteQueue implements ReaderFairByteQueue, WriterFairByteQueue {

  private final WriterFairByteQueue writingDelegate;
  private final ReaderFairByteQueue readingDelegate;

  @Override
  public void close() throws IOException {
    super.close();
    readingDelegate.close();
    writingDelegate.close();
  }

  public static FullyFairByteQueue create(ByteQueue delegate, int maxArrayCopyPerTurn, long maxPollCallsPerTurn, long maxOfferCallsPerTurn) {
    Lock lock = new ReentrantLock(true);
    ReaderFairByteQueue readingDelegate = new ReaderFairByteQueueWrapper(
        delegate, maxArrayCopyPerTurn, maxPollCallsPerTurn, lock);
    WriterFairByteQueue writingDelegate = new WriterFairByteQueueWrapper(
        delegate, maxArrayCopyPerTurn, maxOfferCallsPerTurn, lock);
    return new FullyFairByteQueue(readingDelegate, writingDelegate);
  }

  public FullyFairByteQueue(ReaderFairByteQueue readingDelegate, WriterFairByteQueue writingDelegate) {
    this.readingDelegate = readingDelegate;
    this.writingDelegate = writingDelegate;
  }

  @Override
  public long getCapacity() {
    return readingDelegate.getCapacity();
  }

  @Override
  public int offer(byte[] source, int start, int desiredLength) {
    return writingDelegate.offer(source, start, desiredLength);
  }

  @Override
  public int poll(byte[] dest, int start, int desiredLength) {
    return readingDelegate.poll(dest, start, desiredLength);
  }
}
