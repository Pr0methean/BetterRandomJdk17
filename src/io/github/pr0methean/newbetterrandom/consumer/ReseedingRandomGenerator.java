package io.github.pr0methean.newbetterrandom.consumer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;

import java.util.function.Function;
import java.util.random.RandomGenerator;

public abstract class ReseedingRandomGenerator<T extends RandomGenerator> implements RandomGenerator {
  protected final Function<byte[], T> delegateFactory;
  protected final AtomicSeedByteRingBuffer seedSource;
  protected final int seedSize;
  protected final byte[] seedHolder;
  protected int currentSeedBytes;
  protected T delegate;

  public ReseedingRandomGenerator(
      final Function<byte[], T> delegateFactory, final AtomicSeedByteRingBuffer seedSource, final int seedSize) {
    this.delegateFactory = delegateFactory;
    this.seedSource = seedSource;
    this.seedSize = seedSize;
    seedHolder = new byte[seedSize];
  }

  @Override public long nextLong() {
    maybeReseed();
    return delegate.nextLong();
  }

  protected void maybeReseed() {
    if (delegate == null) {
      setDelegate(createDelegate());
    } else {
      reseedIfImmediatelyPossible();
    }
  }

  protected void reseedIfImmediatelyPossible() {
    tryReseed();
  }

  protected boolean tryReseed() {
    final int bytesRead = seedSource.poll(seedHolder, currentSeedBytes, seedSize - currentSeedBytes);
    if (bytesRead + currentSeedBytes >= seedSize) {
      reseed();
      currentSeedBytes = 0;
      return true;
    } else {
      currentSeedBytes += bytesRead;
      return false;
    }
  }

  protected abstract void reseed();

}
