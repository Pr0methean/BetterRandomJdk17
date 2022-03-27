package io.github.pr0methean.newbetterrandom.producer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;
import java.lang.ref.WeakReference;

public abstract class AbstractSeedProvider implements Runnable {
  protected final WeakReference<AtomicSeedByteRingBuffer> destBuffer;
  protected final int sourceReadSize;

  protected AbstractSeedProvider(final AtomicSeedByteRingBuffer destBuffer, final int sourceReadSize) {
    this.destBuffer = new WeakReference<>(destBuffer);
    this.sourceReadSize = sourceReadSize;
  }

  @Override public void run() {
    AtomicSeedByteRingBuffer destBufferNow;
    while (true) {
      destBufferNow = destBuffer.get();
      if (destBufferNow == null) {
        return; // Dest buffer isn't reachable by potential readers, so terminate the thread
      }
      try {
        byte[] seed = getSeedBytes();
        AtomicSeedByteRingBuffer.writeWhileExists(destBuffer, seed, 0, sourceReadSize);
      } catch (final InterruptedException ignored) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  protected abstract byte[] getSeedBytes() throws InterruptedException;
}
