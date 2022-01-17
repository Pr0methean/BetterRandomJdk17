package io.github.pr0methean.newbetterrandom.consumer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;

import java.util.function.Function;
import java.util.random.RandomGenerator;

public class EntropyBlockingReplacingRandomGenerator<T extends RandomGenerator>
    extends EntropyCountingReplacingRandomGenerator<T> {

  public EntropyBlockingReplacingRandomGenerator(final Function<byte[], T> delegateFactory,
      final AtomicSeedByteRingBuffer seedSource, final int seedSize, final long reseedingThreshold) {
    super(delegateFactory, seedSource, reseedingThreshold, seedSize);
  }

  @Override
  protected void reseedIfImmediatelyPossible() {
    if (getEntropyBits() < reseedingThreshold) {
      while (!tryReseed()) {
        Thread.onSpinWait();
      }
    }
  }
}
