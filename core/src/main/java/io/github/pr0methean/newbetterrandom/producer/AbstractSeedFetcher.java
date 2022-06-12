package io.github.pr0methean.newbetterrandom.producer;

import io.github.pr0methean.newbetterrandom.buffer.ByteQueue;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public abstract class AbstractSeedFetcher implements Runnable, Serializable {
  protected final WeakReference<ByteQueue> destBuffer;
  protected final int sourceReadSize;

  protected AbstractSeedFetcher(final ByteQueue destBuffer, final int sourceReadSize) {
    this.destBuffer = new WeakReference<>(destBuffer);
    this.sourceReadSize = sourceReadSize;
  }

  @Override public void run() {
    ByteQueue destBufferNow;
    while (true) {
      destBufferNow = destBuffer.get();
      if (destBufferNow == null || destBufferNow.isClosed()) {
        return; // Dest buffer is closed or isn't reachable by potential readers, so terminate the thread
      }
      try {
        byte[] seed = getSeedBytes();
        ByteQueue.writeWhileNonNull(((Reference<? extends ByteQueue>) destBuffer)::get, seed, 0, seed.length);
      } catch (final InterruptedException ignored) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  protected abstract byte[] getSeedBytes() throws InterruptedException;
}
