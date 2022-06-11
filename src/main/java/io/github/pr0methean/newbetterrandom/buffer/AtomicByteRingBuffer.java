package io.github.pr0methean.newbetterrandom.buffer;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AtomicByteRingBuffer extends AbstractByteQueue {
  protected final int byteSize;
  protected final int bitMask;
  protected final AtomicLong bytesStartedWriting = new AtomicLong();
  protected final AtomicLong bytesFinishedWriting = new AtomicLong();
  protected final AtomicLong bytesStartedReading = new AtomicLong();

  public AtomicByteRingBuffer(final int byteSize) {
    if (byteSize <= 0) {
      throw new IllegalArgumentException("byteSize must be positive, but is " + byteSize);
    }
    if (Integer.bitCount(byteSize) != 1) {
      throw new IllegalArgumentException("byteSize must be a power of 2, but is " + byteSize);
    }
    this.byteSize = byteSize;
    bitMask = this.byteSize - 1;
  }

  @Override
  public int offer(final byte[] source, final int start, int desiredLength) {
    if (desiredLength < 0) {
      throw new IllegalArgumentException("desiredLength can't be negative");
    }
    if (desiredLength == 0) {
      return 0;
    }
    if (isClosed()) {
      return 0;
    }
    desiredLength = Math.min(desiredLength, byteSize);
    int actualLength = 0;
    final long writeStart = bytesStartedWriting.getAndAdd(desiredLength);
    final long writeLimit = bytesStartedReading.get() + byteSize;
    try {
      final int spaceLeft = (int) (writeLimit - writeStart);
      if (spaceLeft <= 0) {
        return 0; // Buffer is full
      }
      actualLength = Math.min(spaceLeft, desiredLength);
    } finally {
      if (actualLength != desiredLength) {
        bytesStartedWriting.getAndAdd(actualLength - desiredLength);
      }
    }
    final long writeEnd = writeStart + actualLength;
    final int destStartIndex = (int) (writeStart & bitMask);
    final int destEndIndex = (int) (writeEnd & bitMask);
    if (destEndIndex <= destStartIndex) {
      final int beforeWrap = byteSize - destStartIndex;
      final int afterWrap = actualLength - beforeWrap;
      unsafeWrite(destStartIndex, source, start, beforeWrap);
      unsafeWrite(0, source, start + beforeWrap, afterWrap);
    } else {
      unsafeWrite(destStartIndex, source, start, actualLength);
    }
    if (!bytesFinishedWriting.compareAndSet(writeStart, writeEnd)) {
      /*
       * Must start over, to prevent the following scenario:
       *
       * 1. Thread W1 starts writing bytes 0..20. bytesStartedWriting set to 20.
       * 2. Thread W2 starts writing bytes 20..30. bytesStartedWriting set to 30.
       * 3. Thread W2 finishes. bytesFinishedWriting set to 10.
       * 4. Thread R starts reading bytes 0..10, even though they still aren't written.
       */
      bytesStartedWriting.getAndAdd(-actualLength);
      return 0;
    } else {
      return actualLength;
    }
  }

  @Override
  public long getCapacity() {
    return byteSize;
  }

  void checkInternalInvariants() {
    final long finishedWritingTime1 = bytesFinishedWriting.get();
    final long startedWriting = bytesStartedWriting.get();
    if ((int) (finishedWritingTime1 - startedWriting) > 0) {
      throw new AssertionError("bytesStartedWriting < bytesFinishedWriting");
    }
    final long finishedWritingTime2 = bytesFinishedWriting.get();
    if (finishedWritingTime1 > finishedWritingTime2) {
      throw new AssertionError("bytesFinishedWriting is non-monotonic");
    }
  }

  protected abstract void unsafeWrite(int destStart, byte[] source, int sourceStart, int length);

  @Override
  public int poll(final byte[] dest, final int start, int desiredLength) {
    if (desiredLength < 0) {
      throw new IllegalArgumentException("desiredLength can't be negative");
    }
    if (desiredLength == 0) {
      return 0;
    }
    desiredLength = Math.min(desiredLength, byteSize);
    final long readStart = bytesStartedReading.getAndAdd(desiredLength);
    final long written = bytesFinishedWriting.get();
    final int available = (int) (written - readStart);
    if (available <= 0) {
      // Buffer is empty
      bytesStartedReading.getAndAdd(-desiredLength);
      return 0;
    }
    final int actualLength = Math.min(available, desiredLength);
    if (actualLength < desiredLength) {
      bytesStartedReading.getAndAdd(actualLength - desiredLength); // Doing a partial read
    }
    final int readStartIndex = (int) (readStart & bitMask);
    final int readEndIndex = (int) ((readStart + actualLength) & bitMask);
    if (readEndIndex <= readStartIndex) {
      final int lengthBeforeWrap = byteSize - readStartIndex;
      final int lengthAfterWrap = actualLength - lengthBeforeWrap;
      unsafeRead(readStartIndex, dest, start, lengthBeforeWrap);
      unsafeRead(0, dest, start + lengthBeforeWrap, lengthAfterWrap);
    } else {
      unsafeRead(readStartIndex, dest, start, actualLength);
    }
    return actualLength;
  }

  protected abstract void unsafeRead(int sourceStart, byte[] dest, int destStart, int length);
}
