package io.github.pr0methean.newbetterrandom.reseedable;

import static java.lang.Byte.toUnsignedLong;
import static java.lang.Math.scalb;

/**
 * Garbage-free implementation of {@link ReseedableRandomGenerator} whose delegate PRNG is an
 * {@link ArbitrarilyJumpableGenerator}. It is reseeded by jumping a distance equal to the seed.
 */
public class ReseedByArbitraryJumpingRandomGenerator
    implements ReseedableRandomGenerator {
  private static final int BYTES_PER_DOUBLE_WITH_INTEGER_PRECISION = 6;
  protected final ArbitrarilyJumpableGenerator delegate;
  protected final int desiredSeedSizeBytes;

  public ReseedByArbitraryJumpingRandomGenerator(ArbitrarilyJumpableGenerator delegate, final int desiredSeedSizeBytes) {
    this.delegate = delegate;
    this.desiredSeedSizeBytes = desiredSeedSizeBytes;
  }

  @Override
  public void updateSeed(byte[] seed) {
    int lastFullDouble = BYTES_PER_DOUBLE_WITH_INTEGER_PRECISION * (seed.length / BYTES_PER_DOUBLE_WITH_INTEGER_PRECISION);
    int i;
    for (i = 0; i < lastFullDouble; i += BYTES_PER_DOUBLE_WITH_INTEGER_PRECISION) {
      delegate.jump(scalb((double) (
          toUnsignedLong(seed[i]) << 40L
              | toUnsignedLong(seed[i + 1]) << 32L
              | toUnsignedLong(seed[i + 2]) << 24L
              | toUnsignedLong(seed[i + 3]) << 16L
              | toUnsignedLong(seed[i + 4]) << 8L
              | toUnsignedLong(seed[i + 5])), i * Byte.SIZE));
    }
    long lastMantissa = 0;
    for (; i < seed.length; i++) {
      lastMantissa <<= Byte.SIZE;
      lastMantissa |= seed[i];
    }
    if (lastMantissa != 0) {
      delegate.jump(scalb(lastMantissa, lastFullDouble * Byte.SIZE));
    }
  }

  @Override
  public void updateSeed(long seed) {
    double seedAsDouble = seed;
    long remainder = seed - (long)seedAsDouble;
    delegate.jump(seedAsDouble);
    if (remainder != 0) {
      delegate.jump(remainder);
    }
  }

  @Override
  public int seedEntropyBits() {
    return desiredSeedSizeBytes * Byte.SIZE;
  }

  @Override
  public int desiredSeedSizeBytes() {
    return desiredSeedSizeBytes;
  }

  @Override
  public long nextLong() {
    return delegate.nextLong();
  }
}
