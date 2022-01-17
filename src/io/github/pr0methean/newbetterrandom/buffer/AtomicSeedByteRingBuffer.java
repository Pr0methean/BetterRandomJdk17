package io.github.pr0methean.newbetterrandom.buffer;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Byte ring buffer designed to have allocation-free and lock-free offer, allocation-free and wait-free poll,
 * at-most-once delivery, and a single-byte unit of transmission, and to be thread-safe for multiple readers and
 * multiple writerss.</p>
 * <p>The intended use case is reseeding of pseudorandom number generators (PRNGs), to prevent
 * results of complex programs from being influenced by subtle patterns in the PRNG's output. For
 * example, in a simulation program, a handful of threads may read from truly-random sources such as
 * a Unix {@code /dev/random} and call {@link #write(byte[], int, int)} to deliver seed material,
 * while each thread running the simulation calls {@link #poll(byte[], int, int)} and, when it
 * succeeds in fetching enough bytes, either replaces its PRNG with a new one obtained by calling
 * {@link java.util.random.RandomGeneratorFactory#create(byte[])} or, in the case of a
 * {@link java.util.random.RandomGenerator.ArbitrarilyJumpableGenerator} whose period far exceeds
 * the amount of output expected over the simulation's running time, jumping by a distance equal to
 * the seed.</p>
 */
public class AtomicSeedByteRingBuffer {

  public int getByteSize() {
    return byteSize;
  }

  protected final int byteSize;
  protected final int bitMask;

  protected final AtomicLong bytesStartedWriting = new AtomicLong();
  protected final AtomicLong bytesFinishedWriting = new AtomicLong();
  protected final AtomicLong bytesStartedReading = new AtomicLong();
  protected final ByteBuffer buffer;

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

  public AtomicSeedByteRingBuffer(final int byteSize) {
    if (byteSize <= 0) {
      throw new IllegalArgumentException("byteSize must be positive, but is " + byteSize);
    }
    if (Integer.bitCount(byteSize) != 1) {
      throw new IllegalArgumentException("byteSize must be a power of 2, but is " + byteSize);
    }
    this.buffer = ByteBuffer.allocateDirect(byteSize);
    this.byteSize = byteSize;
    bitMask = this.byteSize - 1;
  }

  /**
   * Nonblocking write of up to {@code desiredLength} bytes.
   *
   * @param source the array to copy from
   * @param start the first index to copy from
   * @param desiredLength the maximum number of bytes to write; can be more than this buffer's
   *        capacity, but no more than the capacity will actually be written
   * @param returnOnSpuriousFailure If true, this method may return 0 if there is contention with another writing thread
   *        even when the buffer wasn't full, but will finish in bounded time
   * @return the number of bytes actually written
   */
  public int offer(final byte[] source, final int start, int desiredLength, final boolean returnOnSpuriousFailure) {
    if (desiredLength < 0) {
      throw new IllegalArgumentException("desiredLength can't be negative");
    }
    if (desiredLength == 0) {
      return 0;
    }
    desiredLength = Math.min(desiredLength, byteSize);
    while (true) {
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
        buffer.put(destStartIndex, source, start, beforeWrap);
        buffer.put(0, source, start + beforeWrap, afterWrap);
      } else {
        buffer.put(destStartIndex, source, start, actualLength);
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
        if (returnOnSpuriousFailure) {
          return 0;
        }
      } else {
        return actualLength;
      }
    }
  }

  /**
   * Nonblocking read of up to {@code desiredLength} bytes.
   *
   * @param dest the array to copy into
   * @param start the first index to copy to
   * @param desiredLength the maximum number of bytes to read; can be more than this buffer's
   *        capacity, but no more than the capacity will actually be written
   * @return the number of bytes actually read
   */
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
      buffer.get(readStartIndex, dest, start, lengthBeforeWrap);
      buffer.get(0, dest, start + lengthBeforeWrap, lengthAfterWrap);
    } else {
      buffer.get(readStartIndex, dest, start, actualLength);
    }
    return actualLength;
  }

  /**
   * Blocking write of exactly {@code length} bytes.
   * Deadlock-free provided that enough bytes will eventually be polled for.
   *
   * @param source the array to copy from
   * @param start the first index to copy from
   * @param length the number of bytes to write; may be more than this buffer's capacity
   * @throws InterruptedException if interrupted white waiting to write
   */
  public void write(final byte[] source, final int start, final int length) throws InterruptedException {
    int written = 0;
    while (written < length) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      final int writtenThisIteration = offer(source, start + written, length - written, true);
      if (writtenThisIteration == 0) {
        Thread.onSpinWait();
      }
      written += writtenThisIteration;
    }
  }

  /**
   * Blocking write of exactly {@code length} bytes; returns early if the buffer is garbage-collected before all
   * bytes have been written. Deadlock-free provided that enough bytes will eventually be polled for or the buffer will
   * eventually die.
   *
   * @param bufferRef weak or soft reference to the buffer to write to
   * @param source the array to copy from
   * @param start the first index to copy from
   * @param length the number of bytes to write; may be more than this buffer's capacity
   * @throws InterruptedException if interrupted white waiting to write
   */
  public static int writeWhileExists(final Reference<AtomicSeedByteRingBuffer> bufferRef, final byte[] source,
      final int start, final int length) throws InterruptedException {
    if (bufferRef instanceof PhantomReference) {
      throw new IllegalArgumentException("Can't write using a phantom reference");
    }
    int written = 0;
    while (written < length) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      final AtomicSeedByteRingBuffer buffer = bufferRef.get();
      if (buffer == null) {
        break;
      }
      final int writtenThisIteration = buffer.offer(source, start + written, length - written, true);
      if (writtenThisIteration == 0) {
        Thread.onSpinWait();
      }
      written += writtenThisIteration;
    }
    return written;
  }

  /**
   * Blocking read of exactly {@code length} bytes.
   * Deadlock-free provided that enough bytes will eventually be offered. Starvation is possible if
   * the number of writing threads exceeds the number of physical CPU cores.
   *
   * @param dest the array to copy into
   * @param start the first index to copy to
   * @param length the number of bytes to read; may be more than this buffer's capacity
   * @throws InterruptedException if interrupted while waiting to read
   */
  public void read(final byte[] dest, final int start, final int length) throws InterruptedException {
    int read = 0;
    while (read < length) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      final int readThisIteration = poll(dest, start + read, length - read);
      if (readThisIteration == 0) {
        Thread.onSpinWait();
      }
      read += readThisIteration;
    }
  }
}
