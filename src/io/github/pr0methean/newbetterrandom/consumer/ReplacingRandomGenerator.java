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
}
