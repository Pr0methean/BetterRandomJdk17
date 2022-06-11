package io.github.pr0methean.newbetterrandom.producer;

import io.github.pr0methean.newbetterrandom.buffer.ByteQueue;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public abstract class AbstractSeedProvider implements Runnable {
  protected final WeakReference<ByteQueue> destBuffer;
  protected final int sourceReadSize;

  protected AbstractSeedProvider(final ByteQueue destBuffer, final int sourceReadSize) {
    this.destBuffer = new WeakReference<>(destBuffer);
    this.sourceReadSize = sourceReadSize;
  }

  @Override public void run() {
    ByteQueue destBufferNow;
    while (true) {
      destBufferNow = destBuffer.get();
      if (destBufferNow == null) {
        return; // Dest buffer isn't reachable by potential readers, so terminate the thread
      }
      try {
        byte[] seed = getSeedBytes();
        ByteQueue.writeWhileNonNull(((Reference<? extends ByteQueue>) destBuffer)::get, seed, 0, sourceReadSize);
      } catch (final InterruptedException ignored) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  protected abstract byte[] getSeedBytes() throws InterruptedException;
}
