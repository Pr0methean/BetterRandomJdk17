package io.github.pr0methean.newbetterrandom.producer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;
import java.lang.ref.WeakReference;

public abstract class AbstractSeedProvider implements Runnable {
  protected final WeakReference<AtomicSeedByteRingBuffer> destBuffer;
  protected final byte[] sourceBuffer;
  protected final int sourceReadSize;

  protected AbstractSeedProvider(final int size, final int readConcurrency) {
    this(new AtomicSeedByteRingBuffer(size * readConcurrency), size);
  }

  protected AbstractSeedProvider(final AtomicSeedByteRingBuffer destBuffer, final int sourceReadSize) {
    this.destBuffer = new WeakReference<>(destBuffer);
    this.sourceReadSize = sourceReadSize;
    sourceBuffer = new byte[sourceReadSize];
  }

  @Override public void run() {
    AtomicSeedByteRingBuffer destBufferNow = destBuffer.get();
    try {
      while (destBufferNow != null) {
        fillSourceBuffer();
        AtomicSeedByteRingBuffer.writeWhileExists(destBuffer, sourceBuffer, 0, sourceReadSize);
        destBufferNow = destBuffer.get();
      }
    } catch (final InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }

  protected abstract void fillSourceBuffer();
}
