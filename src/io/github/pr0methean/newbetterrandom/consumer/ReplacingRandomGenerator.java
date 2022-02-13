package io.github.pr0methean.newbetterrandom.consumer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;

import java.util.function.Function;
import java.util.random.RandomGenerator;

public class ReplacingRandomGenerator<T extends RandomGenerator> extends ReseedingRandomGenerator<T> {

  public ReplacingRandomGenerator(final Function<byte[], T> delegateFactory,
      final AtomicSeedByteRingBuffer seedSource, final int seedSize) {
    super(delegateFactory, seedSource, seedSize);
  }

  @Override
  protected void reseed() {
    setDelegate(delegateFactory.apply(seedHolder));
  }

  protected T createDelegate() {
    try {
      seedSource.read(seedHolder, currentSeedBytes, seedSize);
    } catch (final InterruptedException e) {
      throw new RuntimeException(e);
    }
    currentSeedBytes = 0;
    T delegate = delegateFactory.apply(seedHolder);
    return delegate;
  }

  protected void setDelegate(final T newDelegate) {
    delegate = newDelegate;
  }
}
