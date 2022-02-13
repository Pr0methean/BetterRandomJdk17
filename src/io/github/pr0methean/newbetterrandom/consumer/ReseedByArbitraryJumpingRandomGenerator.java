package io.github.pr0methean.newbetterrandom.consumer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;

import java.util.function.Function;
import java.util.random.RandomGenerator.ArbitrarilyJumpableGenerator;

import static java.lang.Math.scalb;

/**
 * Garbage-free implementation of {@link ReseedingRandomGenerator} whose delegate PRNG is an
 * {@link ArbitrarilyJumpableGenerator}. Its entropy is replenished by jumping a truly-random distance.
 * @param <T> the delegate PRNG's type.
 */
public class ReseedByArbitraryJumpingRandomGenerator<T extends ArbitrarilyJumpableGenerator>
    extends ReseedingRandomGenerator<T> {
  /**
   * The maximum number of bytes that can be combined into a long that will always be exactly representable as a double.
   */
  private static final int BYTES_PER_DOUBLE_WITH_INTEGER_PRECISION = 6;

  private final int lastFullDouble = (seedSize / BYTES_PER_DOUBLE_WITH_INTEGER_PRECISION)
      * BYTES_PER_DOUBLE_WITH_INTEGER_PRECISION;

  public ReseedByArbitraryJumpingRandomGenerator(Function<byte[], T> delegateFactory,
                                                 AtomicSeedByteRingBuffer seedSource, int seedSize) {
    super(delegateFactory, seedSource, seedSize);
  }

  @Override
  protected void reseed() {
    int i;
    for (i = 0; i < lastFullDouble; i += BYTES_PER_DOUBLE_WITH_INTEGER_PRECISION) {
      delegate.jump(scalb((double) (((long) (seedHolder[i]) << 40L)
          | ((long) (seedHolder[i + 1]) << 32L)
          | ((long) (seedHolder[i + 2]) << 24L)
          | ((long) (seedHolder[i + 3]) << 16L)
          | ((long) (seedHolder[i + 4]) << 8L)
          | ((long) (seedHolder[i + 5]))), i * Byte.SIZE));
    }
    for (; i < seedSize; i++) {
      delegate.jump(scalb(seedHolder[i], i * Byte.SIZE));
    }
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
